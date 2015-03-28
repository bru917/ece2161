package edu.pitt.ece2161.spring2015.optiplayer;

import android.graphics.Bitmap;

/**
 * This class represents a specific captured video frame. The bitmap holds the
 * captured frame itself, and the position represents the play-time offset
 * when the frame was captured in milliseconds.
 * 
 * @author Brian Rupert
 */
public class CaptureData {

	private Bitmap bitmap;
	
	private long position;
	
	/**
	 * Constructor.
	 * @param b The frame data.
	 * @param pos The position in the video for that frame.
	 */
	public CaptureData(Bitmap b, long pos) {
		this.bitmap = b;
		this.position = pos;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public long getPosition() {
		return position;
	}
}
