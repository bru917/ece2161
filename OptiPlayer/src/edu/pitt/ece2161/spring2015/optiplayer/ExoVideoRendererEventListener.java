package edu.pitt.ece2161.spring2015.optiplayer;

import android.media.MediaCodec.CryptoException;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;

/**
 * Basic event listener for the ExoPlayer video renderer.
 * 
 * @author Brian Rupert
 */
public class ExoVideoRendererEventListener implements MediaCodecVideoTrackRenderer.EventListener {
	
	private static final String TAG = "VEL";
	
	private CustomPlayer player;
	
	ExoVideoRendererEventListener(CustomPlayer player) {
		this.player = player;
	}

	@Override
	public void onCryptoError(CryptoException err) {
		Log.e(TAG, "V.onCryptoError -> " + err);
	}

	@Override
	public void onDecoderInitializationError(DecoderInitializationException err) {
		Log.e(TAG, "V.onDecoderInitializationError -> " + err);
	}

	@Override
	public void onDrawnToSurface(Surface surface) {
		Log.i(TAG, "V.onDrawnToSurface -> " + surface);
	}

	@Override
	public void onDroppedFrames(int count, long elapsed) {
		Log.i(TAG, "V.onDroppedFrames -> " + count + ", " + elapsed);
	}

	@Override
	public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
		Log.i(TAG, "V.onVideoSizeChanged -> " + width + ", " + height + ", " + pixelWidthAspectRatio);
		this.player.getActivityCallback().onVideoSizeChanged(width, height, pixelWidthAspectRatio);	
	}
}
