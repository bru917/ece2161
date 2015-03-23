package edu.pitt.ece2161.spring2015.optiplayer;

import android.graphics.Bitmap;

public class CaptureData {

	private Bitmap bitmap;
	
	private long position;
	
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
