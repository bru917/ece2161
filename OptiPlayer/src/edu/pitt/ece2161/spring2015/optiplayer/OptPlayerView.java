package edu.pitt.ece2161.spring2015.optiplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class OptPlayerView extends SurfaceView {
	
	private boolean customAttr;

	public OptPlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	
	public boolean isCustomAttr() {
		return customAttr;
	}

	public void setCustomAttr(boolean value) {
		customAttr = value;
		invalidate();
		requestLayout();
	}
}
