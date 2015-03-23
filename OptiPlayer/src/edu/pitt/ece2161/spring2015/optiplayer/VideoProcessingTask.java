package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Performs background processing of the video image for low power optimization.
 * 
 * @author Brian Rupert
 *
 */
class VideoProcessingTask extends TimerTask {
	
	public static final int PROCESSING_INTERVAL_MS = 1000;
	
	private static final String TAG = "VideoProcessingTask";
	
	private Context context;
	private CustomView playerView;
	
	private FrameAnalyzer analyzer;
	
	/**
	 * Create the background processing task.
	 * @param surfaceView The view where the video is being rendered.
	 */
	public VideoProcessingTask(Context context, CustomView v) {
		this.playerView = v;
		this.context = context;
		this.analyzer = new FrameAnalyzer(context);
	}
	
	@Override
	public void run() {
		if (this.context == null) {
			return;
		}
		Activity a = (Activity) this.context;
		a.runOnUiThread(new CaptureViewThread());
		//requestCapture();
	}
	
	/**
	 * This thread is used for performing work that must be done on the
	 * UI thread.
	 * 
	 * @author Brian Rupert
	 */
	private class CaptureViewThread extends Thread {
		
		private static final String TAG = "CaptureViewThread";
		
		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				CaptureData cap = playerView.grabFrame();
				Message msg  = Message.obtain(mHandler, CAPTURE_MSG, cap);
				mHandler.sendMessage(msg);
				long time = System.currentTimeMillis() - start;
				Log.v(TAG, "Spent " + time + "ms on UI thread");
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
	}
	
	private static final int CAPTURE_MSG = 123;
	
	/** Handles messages from the CaptureViewThread. */
	private Handler mHandler = new CaptureHandler(this);
	
	/**
	 * Handler class for getting bitmaps off the UI thread and then
	 * doing some processing off the UI thread.
	 * 
	 * @author Brian Rupert
	 */
	private static class CaptureHandler extends Handler {
		
		private VideoProcessingTask parent;
		
		CaptureHandler(VideoProcessingTask parent) {
			this.parent = parent;
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (msg == null || msg.what != CAPTURE_MSG) {
				return;
			}
			CaptureData capData = (CaptureData) msg.obj;
			Log.v(TAG, "Got bitmap from message");
			parent.analyzer.analyze(capData.getBitmap(), capData.getPosition());
			// Need to release the resources used for the bitmap.
			capData.getBitmap().recycle();
		}
	}
	
}
