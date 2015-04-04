package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.TimerTask;

import edu.pitt.ece2161.spring2015.optiplayer.VideoAnalysisDataset.DataEntry;
import android.provider.Settings;
import android.util.Log;

/**
 * Handles backlight adjustment from an existing set of video analysis data.
 * 
 * @author Brian Rupert
 */
class FrameAnalysisPlayback extends TimerTask implements VideoBackgroundTask {
	
	public static final int PROCESSING_INTERVAL_MS = 100;
	
	private static final String TAG = "FrameAnalysisPlayback";

	private VideoAnalysisDataset data;
	private PlayVideoActivity context;
	
	private boolean running = false;
	private int lastValue = -1;
	
	FrameAnalysisPlayback(PlayVideoActivity context, VideoAnalysisDataset data) {
		this.context = context;
		this.data = data;
	}
	
	@Override
	public int getCycleTime() {
		return PROCESSING_INTERVAL_MS;
	}

	@Override
	public void run() {
		if (!running) {
			return;
		}
		
		CustomPlayer player = context.getPlayer();
		if (player == null) {
			return;
		}
		long position = player.getCurrentPosition();
		//Log.d(TAG, "Getting entry for position " + position);
		
		DataEntry entry = data.getEntry(position);
		if (entry == null) {
			return;
		}
		
		final int level = entry.getLevel();
		final int brightnessValue = FrameAnalyzer.getBrightness(level);
		//Log.d(TAG, "Got brightness " + brightnessValue);
		
		if (lastValue != brightnessValue) {
			//Log.d(TAG, "Changing brightness to " + brightnessValue);

			// Only change when needed.
			Settings.System.putInt(context.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
			
			lastValue = brightnessValue;
			
			if (AppSettings.DEBUG) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						context.updateDebugText("Mode: Playback   Level: " + level
								+ "  Brightness: " + brightnessValue);
					}
				});
			}
		}
	}

	@Override
	public void setRunning() {
		Log.d(TAG, "state -> running");
		running = true;
	}

	@Override
	public void setPaused() {
		Log.d(TAG, "state -> paused");
		running = false;
	}

	@Override
	public boolean cancel() {
		// TODO Auto-generated method stub
		return true;
	}

}
