package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;

import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.util.PlayerControl;

/**
 * A video player that wraps the ExoPlayer for playback.
 * 
 * NOTE: Code is derived largely from the ExoPlayer AOSP:
 * https://github.com/google/ExoPlayer
 * 
 *
 */
public class CustomPlayer implements ExoPlayer.Listener {
	
	private static final String TAG = "CustomPlayer";

	private static final int RENDERER_BUILDING_STATE_IDLE = 1;
	private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
	private static final int RENDERER_BUILDING_STATE_BUILT = 3;

	public static final int DISABLED_TRACK = -1;
	public static final int PRIMARY_TRACK = 0;

	public static final int RENDERER_COUNT = 5;
	public static final int TYPE_VIDEO = 0;
	public static final int TYPE_AUDIO = 1;
	public static final int TYPE_TEXT = 2;
	public static final int TYPE_TIMED_METADATA = 3;
	public static final int TYPE_DEBUG = 4;

	private int rendererBuildingState;
	private int lastReportedPlaybackState;
	private boolean lastReportedPlayWhenReady;

	private MultiTrackChunkSource[] multiTrackSources;
	private String[][] trackNames;
	private int[] selectedTracks;
	private boolean backgrounded;

	private final ExoRendererBuilder rendererBuilder;
	private final ExoPlayer player;
	private final PlayerControl playerControl;
	private final Handler mainHandler;

	private Surface surface;
	private TrackRenderer videoRenderer;
	
	private ActivityCallback cbActivity;
	
	public interface ActivityCallback {
		
		void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio);

		void onPlayerError(Throwable t);
	}
	
	public ActivityCallback getActivityCallback() {
		return cbActivity;
	}
	
	private static final int MIN_BUFFER_MS = 1000;
	private static final int MIN_REBUFFER_MS = 5000;

	/**
	 * Creates a player instance.
	 * @param rendererBuilder The renderer builder will build the renderer objects
	 *                        used by the ExoPlayer framework for playback.
	 * @param cbActivity A callback hook for things implemented in the activity.
	 */
	CustomPlayer(ExoRendererBuilder rendererBuilder, ActivityCallback cbActivity) {
		this.rendererBuilder = rendererBuilder;
		this.cbActivity = cbActivity;
		
		player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, MIN_BUFFER_MS, MIN_REBUFFER_MS);
		player.addListener(this);
		playerControl = new PlayerControl(player);
		mainHandler = new Handler();

		// listeners = new CopyOnWriteArrayList<Listener>();
		lastReportedPlaybackState = ExoPlayer.STATE_IDLE;
		rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;

		selectedTracks = new int[RENDERER_COUNT];
		// Disable text initially.
		selectedTracks[TYPE_TEXT] = DISABLED_TRACK;
	}
	
	/**
	 * Gets the underlying exoplayer instance.
	 * @return
	 */
	public ExoPlayer getExo() {
		return this.player;
	}

	/**
	 * Gets the player control object.
	 * @return
	 */
	public PlayerControl getPlayerControl() {
		return playerControl;
	}

	/**
	 * Prepares the internal objects to begin playing video.
	 */
	public void prepare() {
		if (rendererBuildingState == RENDERER_BUILDING_STATE_BUILT) {
			player.stop();
		}
		// if (builderCallback != null) {
		// builderCallback.cancel();
		// }
		rendererBuildingState = RENDERER_BUILDING_STATE_BUILDING;
		// maybeReportPlayerState();
		// builderCallback = new InternalRendererBuilderCallback();
		// rendererBuilder.buildRenderers(this, builderCallback);
		
		rendererBuilder.buildRenderers(this);
	}

	/**
	 * Assigns the surface being used to render the video image content.
	 * @param surface The surface instance.
	 */
	public void setSurface(Surface surface) {
		this.surface = surface;
		pushSurface(false);
	}

	/**
	 * Clears the surface reference, then sends a blocking message to the player
	 * that the surface was cleared.
	 */
	public void blockingClearSurface() {
		surface = null;
		pushSurface(true);
	}

	/**
	 * Push a message to the player informing it of the state of the surface.
	 * @param blockForSurfacePush True to send a blocking message, false for non-blocking.
	 */
	private void pushSurface(boolean blockForSurfacePush) {
		if (rendererBuildingState != RENDERER_BUILDING_STATE_BUILT) {
			return;
		}

		if (blockForSurfacePush) {
			player.blockingSendMessage(videoRenderer,
					MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
		} else {
			player.sendMessage(videoRenderer,
					MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
		}
	}

	/**
	 * Gets the surface used to render the video image content.
	 * @return The surface.
	 */
	public Surface getSurface() {
		return surface;
	}

	public void setPlayWhenReady(boolean playWhenReady) {
		player.setPlayWhenReady(playWhenReady);
	}

	/**
	 * Seek the player to the specified position in milliseconds.
	 * @param positionMs The position to seek to.
	 */
	public void seekTo(long positionMs) {
		player.seekTo(positionMs);
	}

	// ExoPlayer.Listener implementation:

	@Override
	public void onPlayWhenReadyCommitted() {
		// Do nothing.
	}

	@Override
	public void onPlayerError(ExoPlaybackException arg0) {
		Log.e(TAG, "onPlayerError -> " + arg0);
	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		this.lastReportedPlaybackState = playbackState;
		this.lastReportedPlayWhenReady = playWhenReady;
		
		String stateText = String.valueOf(playbackState);
		switch (playbackState) {
		case ExoPlayer.STATE_IDLE:
			stateText = "STATE_IDLE";
			break;
		case ExoPlayer.STATE_PREPARING:
			stateText = "STATE_PREPARING";
			break;
		case ExoPlayer.STATE_BUFFERING:
			stateText = "STATE_BUFFERING";
			break;
		case ExoPlayer.STATE_READY:
			stateText = "STATE_READY";
			break;
		case ExoPlayer.STATE_ENDED:
			stateText = "STATE_ENDED";
			break;
		default:
			break;
		}

		Log.i(TAG, "onPlayerStateChanged -> " + playWhenReady + ", " + stateText);
	}
	
	/**
	 * Gets the last reported value of the playback state.
	 * @return The last reported playback state value.
	 * @see ExoPlayer.STATE_READY
	 */
	public int getLastReportedPlaybackState() {
		return this.lastReportedPlaybackState;
	}
	
	/**
	 * Gets the last reported value of the play-when-ready flag.
	 * @return The last reported play-when-ready flag value.
	 */
	public boolean getLastReportedPlayWhenReady() {
		return this.lastReportedPlayWhenReady;
	}

	Handler getMainHandler() {
		return mainHandler;
	}

	/**
	 * Invoked by renderer builders to assign the renderers they built.
	 * @param trackNames
	 * @param multiTrackSources
	 * @param renderers
	 */
	void onRenderers(String[][] trackNames,
			MultiTrackChunkSource[] multiTrackSources, TrackRenderer[] renderers) {
		// builderCallback = null;
		// Normalize the results.
		if (trackNames == null) {
			trackNames = new String[RENDERER_COUNT][];
		}
		if (multiTrackSources == null) {
			multiTrackSources = new MultiTrackChunkSource[RENDERER_COUNT];
		}
		for (int i = 0; i < RENDERER_COUNT; i++) {
			if (renderers[i] == null) {
				// Convert a null renderer to a dummy renderer.
				renderers[i] = new DummyTrackRenderer();
			} else if (trackNames[i] == null) {
				// We have a renderer so we must have at least one track, but
				// the names are unknown.
				// Initialize the correct number of null track names.
				int trackCount = multiTrackSources[i] == null ? 1
						: multiTrackSources[i].getTrackCount();
				trackNames[i] = new String[trackCount];
			}
		}
		// Complete preparation.
		this.videoRenderer = renderers[TYPE_VIDEO];
		this.trackNames = trackNames;
		this.multiTrackSources = multiTrackSources;
		rendererBuildingState = RENDERER_BUILDING_STATE_BUILT;
		pushSurface(false);
		pushTrackSelection(TYPE_VIDEO, true);
		pushTrackSelection(TYPE_AUDIO, true);
		pushTrackSelection(TYPE_TEXT, true);
		player.prepare(renderers);
	}

	private void pushTrackSelection(int type, boolean allowRendererEnable) {
		if (rendererBuildingState != RENDERER_BUILDING_STATE_BUILT) {
			return;
		}

		int trackIndex = selectedTracks[type];
		if (trackIndex == DISABLED_TRACK) {
			player.setRendererEnabled(type, false);
		} else if (multiTrackSources[type] == null) {
			player.setRendererEnabled(type, allowRendererEnable);
		} else {
			boolean playWhenReady = player.getPlayWhenReady();
			player.setPlayWhenReady(false);
			player.setRendererEnabled(type, false);
			player.sendMessage(multiTrackSources[type],
					MultiTrackChunkSource.MSG_SELECT_TRACK, trackIndex);
			player.setRendererEnabled(type, allowRendererEnable);
			player.setPlayWhenReady(playWhenReady);
		}
	}

	/**
	 * Release player resources.
	 * This method will clear the surface.
	 * The next state will be "idle".
	 */
	public void release() {
		// if (builderCallback != null) {
		// builderCallback.cancel();
		// builderCallback = null;
		// }
		rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
		surface = null;
		player.release();
	}

	private int videoTrackToRestore;

	public int getSelectedTrackIndex(int type) {
		return selectedTracks[type];
	}

	public void selectTrack(int type, int index) {
		if (selectedTracks[type] == index) {
			return;
		}
		selectedTracks[type] = index;
		pushTrackSelection(type, true);
		//if (type == TYPE_TEXT && index == DISABLED_TRACK && textListener != null) {
		//	this.textListener.onText(null);
		//}
	}

	public void setBackgrounded(boolean backgrounded) {
		if (this.backgrounded == backgrounded) {
			return;
		}
		this.backgrounded = backgrounded;
		if (backgrounded) {
			videoTrackToRestore = getSelectedTrackIndex(TYPE_VIDEO);
			selectTrack(TYPE_VIDEO, DISABLED_TRACK);
			blockingClearSurface();
		} else {
			selectTrack(TYPE_VIDEO, videoTrackToRestore);
		}
	}
	
	private ChunkListener chunkListener = new ChunkListener();
	
	/**
	 * Gets the handler for DASH chunk sample events.
	 * @return
	 */
	public ChunkListener getChunkListener() {
		return this.chunkListener;
	}

	/**
	 * This object handles events from a ChunkSampleSource which is used in
	 * the DASH implementation of the ExoPlayer framework.
	 * 
	 * @author Brian Rupert
	 */
	class ChunkListener implements ChunkSampleSource.EventListener {

		@Override
		public void onConsumptionError(int sourceId, IOException e) {
			Log.w(TAG, "onConsumptionError -> " + sourceId + ", " + e);
		}

		@Override
		public void onDownstreamDiscarded(int sourceId, int mediaStartTimeMs,
				int mediaEndTimeMs, long bytesDiscarded) {
			// Do nothing.
		}

		@Override
		public void onDownstreamFormatChanged(int sourceId, String formatId, int trigger, int mediaTimeMs) {
			// Do nothing.
		}

		@Override
		public void onLoadCanceled(int sourceId, long bytesLoaded) {
			// Do nothing.
		}

		@Override
		public void onLoadCompleted(int sourceId, long bytesLoaded) {
			Log.i(TAG, "ChunkListener.onLoadCompleted -> " + sourceId + ", " + bytesLoaded);
		}

		@Override
		public void onLoadStarted(int sourceId, String formatId, int trigger,
				boolean isInitialization, int mediaStartTimeMs, int mediaEndTimeMs, long length) {
			// Do nothing.
		}

		@Override
		public void onUpstreamDiscarded(int sourceId, int mediaStartTimeMs,
				int mediaEndTimeMs, long bytesDiscarded) {
			// Do nothing.
		}

		@Override
		public void onUpstreamError(int sourceId, IOException e) {
			// Do nothing.
		}
	}
	
	private CustomTextRenderer textRenderer = new CustomTextRenderer();
	
	public CustomTextRenderer getTextRenderer() {
		return this.textRenderer;
	}
	
	private class CustomTextRenderer implements TextRenderer {

		@Override
		public void onText(String text) {
			Log.i(TAG, "TextRenderer.onText -> " + text);
		}
		
	}

	public long getCurrentPosition() {
		return player.getCurrentPosition();
	}

	public long getDuration() {
		return player.getDuration();
	}

	public int getBufferedPercentage() {
		return player.getBufferedPercentage();
	}

	public boolean getPlayWhenReady() {
		return player.getPlayWhenReady();
	}
	
	public void onRendererBuilderError(Throwable t) {
		Log.e(TAG, "onRendererBuilderError -> " + t);
		if (cbActivity != null) {
			cbActivity.onPlayerError(t);
		}
	}
	
	private MediaCodecVideoTrackRenderer.EventListener videoListener = new ExoVideoRendererEventListener(this);
	private MediaCodecAudioTrackRenderer.EventListener audioListener = new ExoAudioRendererEventListener();
	
	public MediaCodecVideoTrackRenderer.EventListener getVideoListener() {
		return this.videoListener;
	}
	
	public MediaCodecAudioTrackRenderer.EventListener getAudioListener() {
		return this.audioListener;
	}
}
