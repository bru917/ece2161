package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.Arrays;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.util.Log;

public class FrameAnalyzer {
	
	private static final String TAG = "FrameAnalyzer";

	private ContentResolver cResolver;
	
	private DimmingFileHandler fileHandler;

	public FrameAnalyzer(Context ctx) {
		cResolver = ctx.getContentResolver();
	}

	/**
	 * Perform analysis and adjust backlight level.
	 * @param img The image to analyze.
	 * @param currentPositionMs The current position in the video (ms).
	 */
	public void analyze(Bitmap img, long currentPositionMs) {
		long start = System.currentTimeMillis();
		
		int w = img.getWidth();
		int h = img.getHeight();

		int[] pix = new int[w * h];
		img.getPixels(pix, 0, w, 0, 0, w, h);

		int level = processArray(w, h, pix);

		// Changing the Backlight by the calculated level
		// The Minimum Backlight is 10 and the Maximum Backlight is 255;
		int brightness = getBrightness(level);

		// Log.d("TAG", "brightness"+Integer.toString(brightness));
		// brightness=average/255
		Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
		
		if (fileHandler != null) {
			float pos = (float) currentPositionMs / 1000;
			fileHandler.append(level, pos);
		}
		
		long time = System.currentTimeMillis() - start;
		Log.d(TAG, "Analysis done! duration=" + time + "ms, level=" + level + ", position=" + currentPositionMs + "ms");
	}

	// *************************************************Dimming Algorithm*****************************************************

	public int processArray(int w, int l, int[] array) {
		int level = 0;

		int[][] gray = new int[l][w];
		int[] gray_one = new int[l * w];

		int average = 0;

		for (int i = 0; i < l; i++) {
			for (int j = 0; j < w; j++) {
				int loc = i * w + j;
				int px = array[loc];
				gray[i][j] = (int)
						 ((0.299 * ((px >> 16) & 0xff))
						+ (0.5787 * ((px >> 8) & 0xff))
						+ (0.114 * (px & 0xff)));
				gray_one[loc] = gray[i][j];
				average = average + gray[i][j];
			}
		}
		Arrays.sort(gray_one);

		int median = (gray_one[(l * w) / 2] + gray_one[((l * w) / 2) - 1]) / 2;

		average = (int) (average / (l * w));

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
	private static int getBrightness(int level) {
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
}
