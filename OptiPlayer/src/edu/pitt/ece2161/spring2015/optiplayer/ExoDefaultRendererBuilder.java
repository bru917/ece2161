package edu.pitt.ece2161.spring2015.optiplayer;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;

public class ExoDefaultRendererBuilder implements ExoRendererBuilder {
	
	private Context ctx;
	private Uri uri;
	
	ExoDefaultRendererBuilder(Context ctx, Uri uri) {
		this.ctx = ctx;
		this.uri = uri;
	}

	@Override
	public void buildRenderers(CustomPlayer player) {
		
		// Build the video and audio renderers.
		DefaultSampleSource sampleSource = new DefaultSampleSource(
				new FrameworkSampleExtractor(ctx, uri, null), 2);

		MediaCodecVideoTrackRenderer videoRenderer = null;

		MediaCodecVideoTrackRenderer.EventListener vl = player.getVideoListener();
		MediaCodecAudioTrackRenderer.EventListener al = player.getAudioListener();

		new MediaCodecVideoTrackRenderer(sampleSource, null, true,
				MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, null,
				player.getMainHandler(), vl, 50);


		MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
				sampleSource, null, true, player.getMainHandler(), al);

		// Build the debug renderer.
		TrackRenderer debugRenderer =
		// debugTextView != null ? new DebugTrackRenderer(debugTextView,
		// videoRenderer) : null;
		null;

		TrackRenderer[] renderers = new TrackRenderer[CustomPlayer.RENDERER_COUNT];
		renderers[CustomPlayer.TYPE_VIDEO] = videoRenderer;
		renderers[CustomPlayer.TYPE_AUDIO] = audioRenderer;
		renderers[CustomPlayer.TYPE_DEBUG] = debugRenderer;
		
		player.onRenderers(null, null, renderers);
	}

}
