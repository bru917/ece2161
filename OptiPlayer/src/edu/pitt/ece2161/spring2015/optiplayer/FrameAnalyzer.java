package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.Arrays;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class FrameAnalyzer {
	
	private static final String TAG = "FrameAnalyzer";

	private ContentResolver cResolver;
	
	private VideoAnalysisDataset dataset;
	
	/** This array is used to copy the pixels from a bitmap into for analysis. */
	private int[] pix;

	public FrameAnalyzer(Context ctx) {
		this.cResolver = ctx.getContentResolver();
		this.dataset = new VideoAnalysisDataset();
	}
	
	/**
	 * Perform analysis and adjust backlight level.
	 * @param img The image to analyze.
	 * @param currentPositionMs The current position in the video (ms).
	 */
	public int analyze(Bitmap img, long currentPositionMs) {
		long start = System.currentTimeMillis();
		
		if (AppSettings.DEBUG) {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				// Note that we should AVOID running this on the main thread
				// because it will cause some serious lag on the video.
				Log.w(TAG, "Analysis running on main thread");
			}
		}
		
		int w = img.getWidth();
		int h = img.getHeight();

		int pixCount = w * h;
		if (pix == null || pix.length != pixCount) {
			pix = new int[pixCount];
		}
		// Copy pixels into this flat array for analysis.
		img.getPixels(pix, 0, w, 0, 0, w, h);
		// Process the pixels to compute the screen brightness level.
		int level = processArray(w, h, pix);
		
		// Changing the Backlight by the calculated level
		// The Minimum Backlight is 10 and the Maximum Backlight is 255;
		int brightness = getBrightness(level);
		
		if (AppSettings.DEBUG) {
			long time = System.currentTimeMillis() - start;
			Log.d(TAG, "Analysis done! "
					+ "duration=" + time + "ms, "
					+ "level=" + level + ", "
					+ "brightness=" + brightness + ", "
					+ "position=" + currentPositionMs + "ms");
		}
		
		//long setBrightnessStart = System.currentTimeMillis();
		Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
		//Log.d(TAG, "Set brightness of screen (" + (System.currentTimeMillis() - setBrightnessStart) + "ms)");
		
		if (dataset != null) {
			dataset.update(currentPositionMs, level);
		}
		
		return level;
	}

	// *************************************************Dimming Algorithm*****************************************************
	private static final double COEFF_R = 0.299;
	private static final double COEFF_G = 0.5787;
	private static final double COEFF_B = 0.114;
	
	/**
	 * Processes the array of ARGB values to calculate the level.
	 * @param w Frame width.
	 * @param l Frame height.
	 * @param array The ARGB data.
	 * @return The calculated level.
	 */
	private int processArray(int w, int l, int[] array) {
		int level = 0;
		int totalPixels = l * w;

		int[] gray_one = new int[totalPixels];

		int average = 0;

		for (int i = 0; i < l; i++) {
			for (int j = 0; j < w; j++) {
				int loc = i * w + j;
				int px = array[loc];
				int gray = (int)
						 ((COEFF_R * ((px >> 16) & 0xff))
						+ (COEFF_G * ((px >> 8) & 0xff))
						+ (COEFF_B * (px & 0xff)));
				gray_one[loc] = gray;
				average = average + gray;
			}
		}
		Arrays.sort(gray_one);

		int median = (gray_one[totalPixels / 2] + gray_one[(totalPixels / 2) - 1]) / 2;

		average = (int) (average / totalPixels);

		if (Math.abs(median - average) < 60) {
			level = (int) Math.floor(average / 32);
		}
		return level;
	}

	/**
	 * Gets the brightness setting value given a level (0-8)
	 * @param level
	 * @return
	 */
	public static int getBrightness(int level) {
		int brightness = 10;
		switch (level) {
		case 0:
			brightness = 10;
			break;
		case 1:
			brightness = 45;
			break;
		case 2:
			brightness = 80;
			break;
		case 3:
			brightness = 115;
			break;
		case 4:
			brightness = 150;
			break;
		case 5:
			brightness = 185;
			break;
		case 6:
			brightness = 220;
			break;
		case 7:
			brightness = 255;
			break;
		}
		return brightness;
	}
	
	public VideoAnalysisDataset getDataset() {
		return this.dataset;
	}
}
