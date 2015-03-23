package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public interface CustomView {

	public Surface getSurface();
	
	public void saveFrame(String filename) throws IOException;
	
	public void setVideoWidthHeightRatio(float widthHeightRatio);

	public void onSurfaceReady(SurfaceTexture surface, int width, int height);

	public void onSurfaceSizeChanged(SurfaceTexture surface, int width, int height);
}
