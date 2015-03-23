package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.Timer;
import java.util.UUID;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.google.android.exoplayer.drm.MediaDrmCallback;

import edu.pitt.ece2161.spring2015.optiplayer.ServerCommunicator.CommCallback;
import edu.pitt.ece2161.spring2015.optiplayer.ServerCommunicator.CommStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.MediaController;

/**
 * This activity is used to play the video content.
 * 
 * @author Brian Rupert
 *
 */
public class PlayVideoActivity extends Activity implements CustomPlayer.ActivityCallback {
	
	private static final String TAG = "PlayVideoActivity";
	
	public static final String VIDEO_ID = "VideoID";
	public static final String LOCAL_VIDEO_PATH = "LocalVideoPath";

	//private VideoSurfaceView surfaceView;
	private CustomView surfaceView;
	private MediaController mediaController;

	private CustomPlayer player;
	private boolean playerNeedsPrepare;
	private long playerPosition = 0;
	private boolean enableBackgroundAudio;

	private String videoId;
	private String localVideoFile;
	
	private VideoProcessingTask processingTask;
	private Timer processingTaskTimer;
	
	/**
	 * Gets the surface to be drawn on by the MediaCodec.
	 * @return
	 */
	private Surface getVideoSurface() {
		return surfaceView.getSurface();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play_video);

		final View root = findViewById(R.id.vRoot);
		root.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				// TODO: We get a WindowLeaked error from here when we
				// back-out to the main activity...
				if (!root.isShown()) {
					Log.i(TAG, "onTouch bailing");
					return true;
				}
				
				if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
					toggleControlsVisibility();
				} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
					view.performClick();
				}
				return true;
			}
		});

		// Pull the video ID that should be streamed.
		this.videoId = this.getIntent().getExtras().getString(VIDEO_ID);
		this.localVideoFile = this.getIntent().getExtras().getString(LOCAL_VIDEO_PATH);
		// Without a video ID, there is nothing to wait for or download.
		if (this.videoId != null) {
			// Set up a progress dialog to give the user instant feedback.
			final ProgressDialog progress = new ProgressDialog(this);
			progress.setTitle("Please wait");
			progress.setMessage("Loading video...");
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setProgress(0);
			progress.show();
			// Set up a request to the dimming server.
			ServerCommunicator comm = new ServerCommunicator();
			CommCallback cb = new CommCallback() {
				@Override
				public void execute(CommStatus statusCode, Object data) {
					
					String msg = statusCode.toString();
					if (statusCode == CommStatus.DownloadNotFound) {
						msg = "A dimming file does not exist yet";
					}
	
					// Begin playback on dismissal of the dialog.
					OnDismissListener odl = new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							player.setPlayWhenReady(true);
						}
					};
					
					new AlertDialog.Builder(PlayVideoActivity.this)
							.setTitle("Dimming Information")
							.setMessage(msg)
							.setCancelable(false)
							.setPositiveButton("Play", null)
							.setOnDismissListener(odl)
							.show();
					
					progress.dismiss();
				}
			};
			// Request any existing dimming information.
			comm.download(this, videoId, cb);
		}

		mediaController = new MediaController(this);
		mediaController.setAnchorView(root);
		
		// Where the MediaCodec writes into
		this.surfaceView = (CustomView) findViewById(R.id.vTextureView);
		
		TextureView tv = (TextureView) this.surfaceView;
		tv.setSurfaceTextureListener(new SurfaceTextureListener() {

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				Log.d(TAG, "onSurfaceTextureAvailable() running");
				// Allow the view to set itself up.
				surfaceView.onSurfaceReady(surface, width, height);
				
				// Set-up the player with our surface.
				preparePlayer();
				
				surfaceView.setPlayer(player);
				
				// Set-up background processing.
				if (processingTask == null) {
					processingTaskTimer = new Timer();
					processingTask = new VideoProcessingTask(PlayVideoActivity.this, surfaceView);
					processingTaskTimer.schedule(processingTask, 500, VideoProcessingTask.PROCESSING_INTERVAL_MS);
				}
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
				Log.i(TAG, "onSurfaceTextureSizeChanged -> " + surface + ", " + width + ", " + height);
				surfaceView.onSurfaceSizeChanged(surface, width, height);
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				if (player != null) {
					player.blockingClearSurface();
				}
				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
				// TODO Auto-generated method stub
				
			}
		});

	}

	@Override
	public void onPause() {
		super.onPause();
		if (!enableBackgroundAudio) {
			releasePlayer();
		} else {
			player.setBackgrounded(true);
		}
		// shutterView.setVisibility(View.VISIBLE);
		
		processingTask.cancel();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		releasePlayer();
		
		processingTask.cancel();
	}

	/**
	 * Begins preparing the video player resources.
	 */
	private void preparePlayer() {
		
		String url = VideoUrlProcessor.getDashUrl(this, videoId);
		String id = videoId;

		if (player == null) {
			MediaDrmCallback drmCallback = new MyDrmCallback();

			ExoRendererBuilder rb;
			if (id == null) {
				// Play local video file:
				rb = new ExoDefaultRendererBuilder(this, Uri.parse(this.localVideoFile));
				
			} else {
				// Play video using DASH streaming:
				rb = new ExoDashRendererBuilder(getUserAgent(this), url, id, drmCallback, null);
			}
			
			player = new CustomPlayer(rb, this);

			player.getExo().addListener(new ExoPlayer.Listener() {

				@Override
				public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
					Log.i(TAG, "onPlayerStateChanged -> " + playbackState);
					if (playbackState == ExoPlayer.STATE_ENDED) {
						showControls();
					}
				}

				@Override
				public void onPlayerError(ExoPlaybackException err) {
					Log.e(TAG, "onPlayerError -> " + err);
				}

				@Override
				public void onPlayWhenReadyCommitted() {
					Log.i(TAG, "onPlayWhenReadyCommitted()");
				}
			});

			player.seekTo(playerPosition);
			playerNeedsPrepare = true;
			mediaController.setMediaPlayer(player.getPlayerControl());
			mediaController.setEnabled(true);
		}
		if (playerNeedsPrepare) {
			player.prepare();
			playerNeedsPrepare = false;
			// updateButtonVisibilities();
		}
		player.setSurface(getVideoSurface());
		//player.setPlayWhenReady(true);

	}
	/**
	 * Generates a user agent string to identify our app/platform.
	 * @param context
	 * @return
	 */
	public static String getUserAgent(Context context) {
		String versionName;
		try {
			String packageName = context.getPackageName();
			PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
			versionName = info.versionName;
		} catch (NameNotFoundException e) {
			versionName = "?";
		}
		return "OptiPlayer/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE + ") "
				+ "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
	}

	/**
	 * Releases the player resources.
	 */
	private void releasePlayer() {
		if (player != null) {
			playerPosition = player.getCurrentPosition();
			player.release();
			player = null;
			// eventLogger.endSession();
			// eventLogger = null;
		}
	}

	@Override
	public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
		// shutterView.setVisibility(View.GONE);
		surfaceView.setVideoWidthHeightRatio(height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
	}

	private void toggleControlsVisibility() {
		if (mediaController.isShowing()) {
			mediaController.hide();
			// debugRootView.setVisibility(View.GONE);
		} else {
			showControls();
		}
	}

	private void showControls() {
		mediaController.show(0);
		// debugRootView.setVisibility(View.VISIBLE);
	}

	// SurfaceHolder.Callback implementation

	/*
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (player != null) {
			player.setSurface(getVideoSurface());
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Do nothing.
		Log.i(TAG, "surfaceChanged -> " + holder + ", " + format + ", " + width + ", " + height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (player != null) {
			player.blockingClearSurface();
		}
	}
	*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.play_video, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * TODO: drm handling.
	 */
	private class MyDrmCallback implements MediaDrmCallback {

		@Override
		public byte[] executeKeyRequest(UUID arg0, KeyRequest arg1) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public byte[] executeProvisionRequest(UUID arg0, ProvisionRequest arg1) throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public void onPlayerError(Throwable t) {
		String msg = t.toString();
		if (msg.length() > 200) {
			msg = msg.substring(200) + "...";
		}
		
		new AlertDialog.Builder(this)
	    	.setTitle(t.getClass().getSimpleName())
	    	.setMessage(t.toString())
	    	.show();
	}
}
