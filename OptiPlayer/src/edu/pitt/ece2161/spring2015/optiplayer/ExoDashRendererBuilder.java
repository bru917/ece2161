package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.UnsupportedSchemeException;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleHolder;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.chunk.MediaChunk;
import com.google.android.exoplayer.chunk.MultiTrackChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.ttml.TtmlParser;
import com.google.android.exoplayer.text.webvtt.WebvttParser;
import com.google.android.exoplayer.upstream.BufferPool;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.Util;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;

/**
 * This class is responsible for creating renderers for DASH.
 * 
 * Derived from the ExoPlayer AOSP
 *
 */
public class ExoDashRendererBuilder implements ExoRendererBuilder, ManifestCallback<MediaPresentationDescription> {
	
	private static final String TAG = "ExoDashRendererBuilder";

	private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
	private static final int VIDEO_BUFFER_SEGMENTS = 200;
	private static final int AUDIO_BUFFER_SEGMENTS = 60;
	private static final int TEXT_BUFFER_SEGMENTS = 2;
	private static final int LIVE_EDGE_LATENCY_MS = 30000;

	private static final int SECURITY_LEVEL_UNKNOWN = -1;
	private static final int SECURITY_LEVEL_1 = 1;
	private static final int SECURITY_LEVEL_3 = 3;

	private static final String AC_3_CODEC = "ac-3";
	private static final String E_AC_3_CODEC = "ec-3";

	private final String userAgent;
	private final String url;
	private final String contentId;
	private final MediaDrmCallback drmCallback;
	private final TextView debugTextView;

	private CustomPlayer player;
	// private RendererBuilderCallback callback;
	private ManifestFetcher<MediaPresentationDescription> manifestFetcher;

	public ExoDashRendererBuilder(String userAgent, String url, String contentId, MediaDrmCallback drmCallback,
			TextView debugTextView) {
		this.userAgent = userAgent;
		this.url = url;
		this.contentId = contentId;
		this.drmCallback = drmCallback;
		this.debugTextView = debugTextView;
	}

	@Override
	public void buildRenderers(CustomPlayer player) {
		this.player = player;
		// this.callback = callback;
		MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
		manifestFetcher = new ManifestFetcher<MediaPresentationDescription>(parser, contentId, url, userAgent);
		manifestFetcher.singleLoad(player.getMainHandler().getLooper(), this);
	}

	@Override
	public void onManifestError(String contentId, IOException e) {
		// Report errors up through the player.
		player.onRendererBuilderError(e);
	}

	@Override
	public void onManifest(String contentId, MediaPresentationDescription manifest) {

		MediaCodecVideoTrackRenderer.EventListener vl = this.player.getVideoListener();
		MediaCodecAudioTrackRenderer.EventListener al = this.player.getAudioListener();

		DefaultBandwidthMeter.EventListener l = new DefaultBandwidthMeter.EventListener() {

			@Override
			public void onBandwidthSample(int elapsedMs, long bytes, long bitrateEstimate) {
				// TODO Auto-generated method stub
				Log.i(TAG, "onBandwidthSample -> " + elapsedMs);
			}

		};

		Period period = manifest.periods.get(0);
		Handler mainHandler = player.getMainHandler();
		LoadControl loadControl = new DefaultLoadControl(new BufferPool(BUFFER_SEGMENT_SIZE));
		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mainHandler, l);

		boolean hasContentProtection = false;
		int videoAdaptationSetIndex = period.getAdaptationSetIndex(AdaptationSet.TYPE_VIDEO);
		int audioAdaptationSetIndex = period.getAdaptationSetIndex(AdaptationSet.TYPE_AUDIO);
		AdaptationSet videoAdaptationSet = null;
		AdaptationSet audioAdaptationSet = null;
		if (videoAdaptationSetIndex != -1) {
			videoAdaptationSet = period.adaptationSets.get(videoAdaptationSetIndex);
			hasContentProtection |= videoAdaptationSet.hasContentProtection();
		}
		if (audioAdaptationSetIndex != -1) {
			audioAdaptationSet = period.adaptationSets.get(audioAdaptationSetIndex);
			hasContentProtection |= audioAdaptationSet.hasContentProtection();
		}

		// Fail if we have neither video or audio.
		if (videoAdaptationSet == null && audioAdaptationSet == null) {
			player.onRendererBuilderError(new IllegalStateException("No video or audio adaptation sets"));
			return;
		}

		// Check drm support if necessary.
		boolean filterHdContent = false;
		DrmSessionManager drmSessionManager = null;
		if (hasContentProtection) {
			if (Util.SDK_INT < 18) {
				player.onRendererBuilderError(new UnsupportedDrmException(UnsupportedDrmException.REASON_NO_DRM));
				return;
			}
			try {
				Pair<DrmSessionManager, Boolean> drmSessionManagerData = V18Compat.getDrmSessionManagerData(player,
						drmCallback);
				drmSessionManager = drmSessionManagerData.first;
				// HD streams require L1 security.
				filterHdContent = videoAdaptationSet != null && videoAdaptationSet.hasContentProtection()
						&& !drmSessionManagerData.second;
			} catch (UnsupportedDrmException e) {
				player.onRendererBuilderError(e);
				return;
			}
		}

		// Determine which video representations we should use for playback.
		ArrayList<Integer> videoRepresentationIndexList = new ArrayList<Integer>();
		if (videoAdaptationSet != null) {
			int maxDecodableFrameSize;
			try {
				maxDecodableFrameSize = MediaCodecUtil.maxH264DecodableFrameSize();
			} catch (DecoderQueryException e) {
				player.onRendererBuilderError(e);
				return;
			}
			List<Representation> videoRepresentations = videoAdaptationSet.representations;
			for (int i = 0; i < videoRepresentations.size(); i++) {
				Format format = videoRepresentations.get(i).format;
				if (filterHdContent && (format.width >= 1280 || format.height >= 720)) {
					// Filtering HD content
				} else if (format.width * format.height > maxDecodableFrameSize) {
					// Filtering stream that device cannot play
				} else if (!format.mimeType.equals(MimeTypes.VIDEO_MP4)
						&& !format.mimeType.equals(MimeTypes.VIDEO_WEBM)) {
					// Filtering unsupported mime type
				} else {
					videoRepresentationIndexList.add(i);
				}
			}
		}

		// Build the video renderer.
		final MediaCodecVideoTrackRenderer videoRenderer;
		final TrackRenderer debugRenderer;
		if (videoRepresentationIndexList.isEmpty()) {
			videoRenderer = null;
			debugRenderer = null;
		} else {
			int[] videoRepresentationIndices = Util.toArray(videoRepresentationIndexList);
			
			DataSource videoDataSource = new UriDataSource(userAgent, bandwidthMeter);
			
			ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher, videoAdaptationSetIndex,
					videoRepresentationIndices, videoDataSource, new AdaptiveEvaluator(bandwidthMeter),
					LIVE_EDGE_LATENCY_MS);
			
			ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
					VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, true, mainHandler, player.getChunkListener(),
					CustomPlayer.TYPE_VIDEO) {
				
				@Override
				protected void onSampleRead(MediaChunk mediaChunk, SampleHolder sampleHolder) {
					// Happens when media samples come in from the stream.
				}
			};
			
			videoRenderer = new MediaCodecVideoTrackRenderer(videoSampleSource, drmSessionManager, true,
					MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null, mainHandler, vl, 50);
			
			//debugRenderer = debugTextView != null
			//		? new DebugTrackRenderer(debugTextView, videoRenderer, videoSampleSource) : null;
			 
		}

		// Build the audio chunk sources.
		boolean haveAc3Tracks = false;
		List<ChunkSource> audioChunkSourceList = new ArrayList<ChunkSource>();
		List<String> audioTrackNameList = new ArrayList<String>();
		if (audioAdaptationSet != null) {
			DataSource audioDataSource = new UriDataSource(userAgent, bandwidthMeter);
			FormatEvaluator audioEvaluator = new FormatEvaluator.FixedEvaluator();
			List<Representation> audioRepresentations = audioAdaptationSet.representations;
			for (int i = 0; i < audioRepresentations.size(); i++) {
				Format format = audioRepresentations.get(i).format;
				audioTrackNameList.add(format.id + " (" + format.numChannels + "ch, " + format.audioSamplingRate
						+ "Hz)");
				audioChunkSourceList.add(new DashChunkSource(
						manifestFetcher, audioAdaptationSetIndex, new int[] { i },
						audioDataSource, audioEvaluator, LIVE_EDGE_LATENCY_MS));
				haveAc3Tracks |= AC_3_CODEC.equals(format.codecs) || E_AC_3_CODEC.equals(format.codecs);
			}
			// Filter out non-AC-3 tracks if there is an AC-3 track, to avoid
			// having to switch renderers.
			if (haveAc3Tracks) {
				for (int i = audioRepresentations.size() - 1; i >= 0; i--) {
					Format format = audioRepresentations.get(i).format;
					if (!AC_3_CODEC.equals(format.codecs) && !E_AC_3_CODEC.equals(format.codecs)) {
						audioTrackNameList.remove(i);
						audioChunkSourceList.remove(i);
					}
				}
			}
		}

		// Build the audio renderer.
		final String[] audioTrackNames;
		final MultiTrackChunkSource audioChunkSource;
		final TrackRenderer audioRenderer;
		if (audioChunkSourceList.isEmpty()) {
			audioTrackNames = null;
			audioChunkSource = null;
			audioRenderer = null;
		} else {
			audioTrackNames = new String[audioTrackNameList.size()];
			audioTrackNameList.toArray(audioTrackNames);
			audioChunkSource = new MultiTrackChunkSource(audioChunkSourceList);
			SampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl, AUDIO_BUFFER_SEGMENTS
					* BUFFER_SEGMENT_SIZE, true, mainHandler, player.getChunkListener(), CustomPlayer.TYPE_AUDIO);
			audioRenderer = new MediaCodecAudioTrackRenderer(
					audioSampleSource, drmSessionManager, true, mainHandler, al);
		}

		// Build the text chunk sources.
		DataSource textDataSource = new UriDataSource(userAgent, bandwidthMeter);
		FormatEvaluator textEvaluator = new FormatEvaluator.FixedEvaluator();
		List<ChunkSource> textChunkSourceList = new ArrayList<ChunkSource>();
		List<String> textTrackNameList = new ArrayList<String>();
		for (int i = 0; i < period.adaptationSets.size(); i++) {
			AdaptationSet adaptationSet = period.adaptationSets.get(i);
			if (adaptationSet.type == AdaptationSet.TYPE_TEXT) {
				List<Representation> representations = adaptationSet.representations;
				for (int j = 0; j < representations.size(); j++) {
					Representation representation = representations.get(j);
					textTrackNameList.add(representation.format.id);
					textChunkSourceList.add(new DashChunkSource(manifestFetcher, i, new int[] { j }, textDataSource,
							textEvaluator, LIVE_EDGE_LATENCY_MS));
				}
			}
		}

		// Build the text renderers
		final String[] textTrackNames;
		final MultiTrackChunkSource textChunkSource;
		final TrackRenderer textRenderer;
		if (textChunkSourceList.isEmpty()) {
			textTrackNames = null;
			textChunkSource = null;
			textRenderer = null;
		} else {
			textTrackNames = new String[textTrackNameList.size()];
			textTrackNameList.toArray(textTrackNames);
			textChunkSource = new MultiTrackChunkSource(textChunkSourceList);
			
			SampleSource textSampleSource = new ChunkSampleSource(textChunkSource, loadControl, TEXT_BUFFER_SEGMENTS
					* BUFFER_SEGMENT_SIZE, true, mainHandler, player.getChunkListener(), CustomPlayer.TYPE_TEXT);

			textRenderer = new TextTrackRenderer(textSampleSource,
					player.getTextRenderer(), mainHandler.getLooper(), new TtmlParser(), new WebvttParser());
		}

		// Invoke the callback.
		String[][] trackNames = new String[CustomPlayer.RENDERER_COUNT][];
		trackNames[CustomPlayer.TYPE_AUDIO] = audioTrackNames;
		// trackNames[CustomPlayer.TYPE_TEXT] = textTrackNames;

		MultiTrackChunkSource[] multiTrackChunkSources = new MultiTrackChunkSource[CustomPlayer.RENDERER_COUNT];
		multiTrackChunkSources[CustomPlayer.TYPE_AUDIO] = audioChunkSource;
		// multiTrackChunkSources[CustomPlayer.TYPE_TEXT] = textChunkSource;

		TrackRenderer[] renderers = new TrackRenderer[CustomPlayer.RENDERER_COUNT];
		renderers[CustomPlayer.TYPE_VIDEO] = videoRenderer;
		renderers[CustomPlayer.TYPE_AUDIO] = audioRenderer;
		// renderers[CustomPlayer.TYPE_TEXT] = textRenderer;
		// renderers[CustomPlayer.TYPE_DEBUG] = debugRenderer;
		player.onRenderers(trackNames, multiTrackChunkSources, renderers);
	}

	@TargetApi(18)
	private static class V18Compat {

		public static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);

		public static Pair<DrmSessionManager, Boolean> getDrmSessionManagerData(CustomPlayer player,
				MediaDrmCallback drmCallback) throws UnsupportedDrmException {
			try {
				StreamingDrmSessionManager streamingDrmSessionManager = new StreamingDrmSessionManager(WIDEVINE_UUID,
						player.getExo().getPlaybackLooper(), drmCallback, null, player.getMainHandler(),
						// player);
						null);
				return Pair.create((DrmSessionManager) streamingDrmSessionManager,
						getWidevineSecurityLevel(streamingDrmSessionManager) == SECURITY_LEVEL_1);
			} catch (UnsupportedSchemeException e) {
				throw new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME);
			} catch (Exception e) {
				throw new UnsupportedDrmException(UnsupportedDrmException.REASON_UNKNOWN, e);
			}
		}

		private static int getWidevineSecurityLevel(StreamingDrmSessionManager sessionManager) {
			String securityLevelProperty = sessionManager.getPropertyString("securityLevel");
			return securityLevelProperty.equals("L1") ? SECURITY_LEVEL_1
					: securityLevelProperty.equals("L3") ? SECURITY_LEVEL_3 : SECURITY_LEVEL_UNKNOWN;
		}

	}

}
