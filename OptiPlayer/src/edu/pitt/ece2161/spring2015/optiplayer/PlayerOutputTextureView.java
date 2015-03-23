package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.android.exoplayer.VideoSurfaceView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

/**
 * This view is used to display the video content and facilitates the capture
 * of the video content for processing.
 * 
 * @author Brian Rupert
 */
public class PlayerOutputTextureView extends TextureView implements CustomView {
	
	private static final String TAG = "PlayerOutputTextureView";
	
	private static final boolean DEBUG = true;
	private static final boolean SAVE_IMG = false;
	
	private int mHeight;
	private int mWidth;
	private ByteBuffer mPixelBuf;
	
	// TextureView
	private SurfaceTexture mSurfaceTexture;
	private Surface mSurface;

	/**
	 * Constructor.
	 * @param context
	 */
	public PlayerOutputTextureView(Context context) {
		super(context);
		init();
	}
	
	/**
	 * Constructor.
	 * @param context
	 * @param attrs
	 */
	public PlayerOutputTextureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	/**
	 * Initialization common to various constructors.
	 */
	private void init() {
		
	}
	

	@Override
	public void onSurfaceReady(SurfaceTexture surface, int width, int height) {
		// The underlying surface texture of this TextureView should be ready.
		mSurfaceTexture = this.getSurfaceTexture();
		if (mSurfaceTexture == null) {
			throw new IllegalStateException("SurfaceTexture is not prepared!");
		}
		Log.i(TAG, "Initializing surface (" + width + "x" + height + ")");
		// Make a new surface associated with the SurfaceTexture.
		// This surface is written to by MediaCodec via the ExoPlayer TrackRenderer magic.
		mSurface = new Surface(mSurfaceTexture);
		
		// Necessary???
		this.setDrawingCacheEnabled(true);
		this.buildDrawingCache();
		
		// Generate a buffer to facilitate the conversion of the screen pixels
		// into a more usable bitmap instance.
		mHeight = 1024;
		mWidth = 768;
		this.allocateBuffer(mWidth, mHeight);
	}

	@Override
	public void saveFrame(String filename) throws IOException {
		Bitmap img = captureGL();
		if (img != null) {
			Log.i(TAG, "GOT BITMAP! = " + img);
			
			if (SAVE_IMG) {
				// Write the captured bitmap to a file.
				final Bitmap fImg = Bitmap.createBitmap(img);
				final String fFileName = filename;
				Thread t = new Thread() {
					public void run() {
						try {
							BitmapUtil.saveBitmap(fImg, fFileName);
							fImg.recycle();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				};
				t.start();
			}
			
			// Blow-away the bitmap object we generated.
			img.recycle();
		}
	}
	
	/**
	 * Captures the current screen's display, however this only captures what
	 * is available in GLES space. No hardware-based surfaces can be captured
	 * using this method.
	 * 
	 * @return The bitmap of the display.
	 */
	private Bitmap captureGL() {
		if (mPixelBuf == null) {
			Log.w(TAG, "Byte buffer undefined");
			return null;
		}
		long start = 0;
		if (DEBUG) {
			start = System.currentTimeMillis();
		}
		
		// Ensure the buffer is reset to the start.
		mPixelBuf.rewind();
		// Pull the displayed pixels from GLES.
		// The video content is included only when it uses TextureView.
		GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);
		// Generate a bitmap to copy the pixels into.
		Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		// Reset the buffer to the start again.
		mPixelBuf.rewind();
		// Copy the pixels into the bitmap.
		bmp.copyPixelsFromBuffer(mPixelBuf);
		
		if (DEBUG) {
			long time = System.currentTimeMillis() - start;
			Log.d(TAG, "Screen capture completed in " + time + "ms");
		}
		
		return bmp;
	}
	
	/**
	 * Allocates the buffer where the screen capture bits are stored to
	 * facilitate creating a bitmap object.
	 * @param width Buffered image width
	 * @param height Buffered image height
	 */
	private void allocateBuffer(int width, int height) {
		if (mPixelBuf != null) {
			// Any manual releasing?
			mPixelBuf = null;
		}
		mPixelBuf = ByteBuffer.allocateDirect(width * height * 4);
		mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
		
		Log.d(TAG, "Pixel buffer allocated (" + mPixelBuf.capacity() + " bytes)");
	}
	
	/**
	 * Returns the surface where the video is displayed.
	 */
	public Surface getSurface() {
		return this.mSurface;
	}
	
//------------------------------------------------------------------------------
//    Taken from ExoPlayer demo code:
//------------------------------------------------------------------------------

	/**
	 * The surface view will not resize itself if the fractional difference
	 * between its default aspect ratio and the aspect ratio of the video falls
	 * below this threshold.
	 * <p>
	 * This tolerance is useful for fullscreen playbacks, since it ensures that
	 * the surface will occupy the whole of the screen when playing content that
	 * has the same (or virtually the same) aspect ratio as the device. This
	 * typically reduces the number of view layers that need to be composited by
	 * the underlying system, which can help to reduce power consumption.
	 */
	private static final float MAX_ASPECT_RATIO_DEFORMATION_PERCENT = 0.01f;
	
	private float videoAspectRatio;
	
	/**
	 * Set the aspect ratio that this {@link VideoSurfaceView} should satisfy.
	 *
	 * @param widthHeightRatio  The width to height ratio.
	 */
	public void setVideoWidthHeightRatio(float widthHeightRatio) {
		if (this.videoAspectRatio != widthHeightRatio) {
			this.videoAspectRatio = widthHeightRatio;
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		if (videoAspectRatio != 0) {
			float viewAspectRatio = (float) width / height;
			float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
			if (aspectDeformation > MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
				height = (int) (width / videoAspectRatio);
			} else if (aspectDeformation < -MAX_ASPECT_RATIO_DEFORMATION_PERCENT) {
				width = (int) (height * videoAspectRatio);
			}
		}
		setMeasuredDimension(width, height);
	}

}
