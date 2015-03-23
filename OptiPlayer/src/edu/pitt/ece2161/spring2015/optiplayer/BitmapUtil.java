package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Utility class for bitmap handling.
 * 
 * @author Brian Rupert
 */
public class BitmapUtil {
	
	private static final String TAG = "BitmapUtil";
	
	/**
	 * Compresses and saves a bitmap to a file.
	 * @param bmp
	 * @param filename
	 * @throws IOException
	 */
	public static void saveBitmap(Bitmap bmp, String filename) throws IOException {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(filename));
			bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
		} finally {
			if (bos != null) {
				bos.close();
			}
		}
		Log.d(TAG, "Saved " + bmp.getWidth() + "x" + bmp.getHeight() + " frame as '" + filename + "'");
	}
}
