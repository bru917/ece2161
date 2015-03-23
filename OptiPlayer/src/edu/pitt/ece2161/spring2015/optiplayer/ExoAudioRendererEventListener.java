package edu.pitt.ece2161.spring2015.optiplayer;

import android.media.MediaCodec.CryptoException;
import android.util.Log;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.audio.AudioTrack.InitializationException;
import com.google.android.exoplayer.audio.AudioTrack.WriteException;

/**
 * Handles events from the audio renderer in the ExoPlayer framework.
 * 
 * @author Brian Rupert
 */
public class ExoAudioRendererEventListener implements MediaCodecAudioTrackRenderer.EventListener {
	
	private static final String TAG = "AEL";

	@Override
	public void onCryptoError(CryptoException err) {
		Log.e(TAG, "A.onCryptoError -> " + err);
	}

	@Override
	public void onDecoderInitializationError(DecoderInitializationException err) {
		Log.e(TAG, "A.onDecoderInitializationError -> " + err);
	}

	@Override
	public void onAudioTrackInitializationError(InitializationException err) {
		Log.e(TAG, "A.onAudioTrackInitializationError -> " + err);
	}

	@Override
	public void onAudioTrackWriteError(WriteException err) {
		Log.e(TAG, "A.onAudioTrackWriteError -> " + err);
	}

}
