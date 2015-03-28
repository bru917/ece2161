package edu.pitt.ece2161.spring2015.optiplayer;

import java.text.NumberFormat;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
			Log.w(TAG, "The context is not set! bailing...");
			return;
		}
		
		if (backgroundHandler == null) {
			handlerSetupThread.start();
		}
		Log.i(TAG, "Invoking capture thread");
		
		Activity a = (Activity) this.context;
		a.runOnUiThread(new CaptureViewThread());
		//requestCapture();
	}
	
	private Thread handlerSetupThread = new Thread(new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            backgroundHandler = new CaptureHandler(VideoProcessingTask.this);
            Message message = backgroundHandler.obtainMessage();
            backgroundHandler.dispatchMessage(message);
            Looper.loop();
        }
    });
	
	@Override
	public boolean cancel() {
		boolean b = super.cancel();
		if (backgroundHandler != null) {
			backgroundHandler.cancel();
			backgroundHandler = null;
		}
		return b;
	}

	/**
	 * This method will obtain the current video frame, then send a message to
	 * the handler we delegated for analysis work, which runs on another thread.
	 * <strong>This must run on the main UI thread.</strong>
	 */
	private void doWork() {
		if (backgroundHandler == null) {
			Log.w(TAG, "Handler for analysis is not set! Skipping capture");
			return;
		}
		
		//Log.d(TAG, "doWork() on thread " + getCurrentThreadName());
				
		long start = System.nanoTime();
		try {
			// Currently on UI thread... We can grab the frame here.
			CaptureData cap = playerView.grabFrame();
			// Now go to background thread... Do analysis there.
			Message msg = Message.obtain(backgroundHandler, CAPTURE_MSG, cap);
			backgroundHandler.sendMessage(msg);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		if (AppSettings.DEBUG) {
			long timeNs = System.nanoTime() - start;
			double timeMs = timeNs / 1000000;
			Log.v(TAG, "Spent " + NumberFormat.getNumberInstance().format(timeMs) + "ms on UI thread");
		}
	}
	
	/**
	 * This thread is used for performing work that must be done on the UI thread.
	 * 
	 * @author Brian Rupert
	 */
	private class CaptureViewThread extends Thread {
		
		@Override
		public void run() {
			doWork();
		}
	}
	
	private static final int CAPTURE_MSG = 123;
	
	/** Handles messages for background analysis. */
	private CaptureHandler backgroundHandler;
	
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
			if (AppSettings.DEBUG) {
				Log.v(TAG, "Got bitmap from message, starting analysis on thread " + getCurrentThreadName());
			}
			parent.analyzer.analyze(capData.getBitmap(), capData.getPosition());
			// Need to release the resources used for the bitmap.
			capData.getBitmap().recycle();
		}
		
		private void cancel() {
			this.removeCallbacksAndMessages(null);
		}
	}
	
	/**
	 * Returns the name of the current thread.
	 * @return {@code null} if a looper is not associated with the current thread,
	 *         {@code "?"} if the current looper is not associated with a thread,
	 *         otherwise, the thread name.
	 */
	public static String getCurrentThreadName() {
		String threadName = null;
		Looper l = Looper.myLooper();
		if (l != null) {
			Thread t = l.getThread();
			if (t != null) {
				threadName = t.getName();
			} else {
				threadName = "?";
			}
		}
		return threadName;
	}
}
