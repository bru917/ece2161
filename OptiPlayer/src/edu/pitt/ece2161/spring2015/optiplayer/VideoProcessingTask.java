package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Performs background processing of the video image for low power optimization.
 * 
 * @author Brian Rupert
 *
 */
class VideoProcessingTask extends TimerTask {
	
	public static final int PROCESSING_INTERVAL_MS = 3000;
	
	private Context context;
	private CustomView playerView;
	
	/**
	 * Create the background processing task.
	 * @param surfaceView The view where the video is being rendered.
	 */
	public VideoProcessingTask(Context context, CustomView v) {
		this.playerView = v;
		this.context = context;
	}
	
	@Override
	public void run() {
		if (this.context == null) {
			return;
		}
		Activity a = (Activity) this.context;
		a.runOnUiThread(new CaptureViewThread());
	}
	
	private static String getFileName(int i) {
		return Environment.getExternalStorageDirectory().getPath() + "/video_" + i + ".png";
	}
	
	int captureCount = 0;
	
	private class CaptureViewThread extends Thread {
		
		@Override
		public void run() {
			try {
				playerView.saveFrame(getFileName(captureCount));
			} catch (IOException e) {
				Log.e("CaptureViewThread", e.toString());
			}
			captureCount++;
		}
	}
}
