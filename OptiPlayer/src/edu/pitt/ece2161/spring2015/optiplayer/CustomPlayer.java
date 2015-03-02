package edu.pitt.ece2161.spring2015.optiplayer;

import android.os.Handler;
import android.view.Surface;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.util.PlayerControl;

/**
 * NOTE: Code is derived largely from the ExoPlayer project:
 * https://github.com/google/ExoPlayer
 * 
 *
 */
public class CustomPlayer implements ExoPlayer.Listener {

	private static final int RENDERER_BUILDING_STATE_IDLE = 1;
	private static final int RENDERER_BUILDING_STATE_BUILDING = 2;
	private static final int RENDERER_BUILDING_STATE_BUILT = 3;

	private static final int RENDERER_COUNT = 2;

	private int rendererBuildingState;
	private int lastReportedPlaybackState;
	private boolean lastReportedPlayWhenReady;

	// private final RendererBuilder rendererBuilder;
	private final ExoPlayer player;
	private final PlayerControl playerControl;
	private final Handler mainHandler;

	private Surface surface;
	private TrackRenderer videoRenderer;

	// private final CopyOnWriteArrayList<Listener> listeners;

	CustomPlayer() {
		// this.rendererBuilder = rendererBuilder;
		player = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
		player.addListener(this);
		playerControl = new PlayerControl(player);
		mainHandler = new Handler();

		// listeners = new CopyOnWriteArrayList<Listener>();
		lastReportedPlaybackState = ExoPlayer.STATE_IDLE;
		rendererBuildingState = RENDERER_BUILDING_STATE_IDLE;
	}

	public PlayerControl getPlayerControl() {
		return playerControl;
	}

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
	}

	public void setSurface(Surface surface) {
		this.surface = surface;
		pushSurface(false);
	}

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

	public Surface getSurface() {
		return surface;
	}

	public void setPlayWhenReady(boolean playWhenReady) {
		player.setPlayWhenReady(playWhenReady);
	}

	public void seekTo(long positionMs) {
		player.seekTo(positionMs);
	}

	// ExoPlayer.Listener:

	@Override
	public void onPlayWhenReadyCommitted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPlayerError(ExoPlaybackException arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPlayerStateChanged(boolean arg0, int arg1) {
		// TODO Auto-generated method stub

	}
}
