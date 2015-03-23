package edu.pitt.ece2161.spring2015.optiplayer;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * Defines methods important for whatever view is being used to display the
 * video content within the activity.
 * 
 * @author Brian Rupert
 */
public interface CustomView {

	public Surface getSurface();
	
	public CaptureData grabFrame();
	
	public void setVideoWidthHeightRatio(float widthHeightRatio);

	public void onSurfaceReady(SurfaceTexture surface, int width, int height);

	public void onSurfaceSizeChanged(SurfaceTexture surface, int width, int height);

	public void setPlayer(CustomPlayer player);
}
