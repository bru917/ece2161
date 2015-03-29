package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.FileNotFoundException;
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
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
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
import android.widget.TextView;

/**
 * This activity is used to play the video content.
 * 
 * @author Brian Rupert
 *
 */
public class PlayVideoActivity extends Activity
implements CustomPlayer.ActivityCallback, ExoPlayer.Listener {
	
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
	
	private TextView debugText;
	
	private VideoProcessingTask processingTask;
	private Timer processingTaskTimer;
	
	private Integer origBrightnessSetting;
	
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
		
		Log.i(TAG, "Activity is being CREATED");
		
		// Remember the original brightness setting, to restore it later.
		try {
			origBrightnessSetting = Settings.System.getInt(
					getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
			Log.d(TAG, "Saved original brightness level of " + this.origBrightnessSetting);
		} catch (SettingNotFoundException e) {
			origBrightnessSetting = null;
		}

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
		
		if (AppSettings.DEBUG) {
			debugText = (TextView) this.findViewById(R.id.tDebugText);
			debugText.setVisibility(View.VISIBLE);
		}

		// Pull the video ID that should be streamed.
		this.videoId = this.getIntent().getExtras().getString(VIDEO_ID);
		this.localVideoFile = this.getIntent().getExtras().getString(LOCAL_VIDEO_PATH);
		// Without a video ID, there is nothing to wait for or download.
		if (this.videoId != null) {
			requestDimmingFile(videoId);
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
				//Log.v(TAG, "onSurfaceTextureUpdated -> " + surface);
			}
		});

	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.i(TAG, "Activity is being STARTED");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "Activity is being RESUMED");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i(TAG, "Activity is being PAUSED");
		
		if (!enableBackgroundAudio) {
			releasePlayer();
		} else {
			player.setBackgrounded(true);
		}
		
		setAnalysisMode(AnalysisMode.PAUSE);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "Activity is being STOPPED");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Activity is being DESTROYED");
		
		if (this.origBrightnessSetting != null) {
			Log.d(TAG, "Restoring brightness level to " + this.origBrightnessSetting);
			boolean ok = Settings.System.putInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS, this.origBrightnessSetting);
			if (!ok) {
				Log.w(TAG, "Failed to restore brightness setting");
			}
		}
		
		releasePlayer();
		
		setAnalysisMode(AnalysisMode.STOP);
	}
	
	/**
	 * Displays a progress dialog and begins the process of contacting the
	 * dimming file server. When the dimming file server responds, a callback
	 * takes action.
	 * 
	 * If the dimming file was not found, a dialog is displayed
	 * and the user can hit the 'Play' button to continue playing which will
	 * also begin the video analysis.
	 * 
	 * If the dimming file was found, the file data is read and the background
	 * dimming service is configured automatically, then the video begins as
	 * soon as it is ready.
	 * 
	 * @param videoId The ID of the video we are requesting.
	 */
	private void requestDimmingFile(String videoId) {
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
				} else if (statusCode == CommStatus.DownloadOk) {
					// Show nothing, just start playing.
					msg = null;
				}
				
				if (msg != null) {
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
				} else {
					// TODO assign downloaded file into the playback
					// background service.
					
					player.setPlayWhenReady(true);
				}
				
				progress.dismiss();
			}
		};
		// Request any existing dimming information.
		comm.download(this, videoId, cb);
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
			player.getExo().addListener(this);
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
		player.setPlayWhenReady(false);
	}
	
	public enum AnalysisMode {
		RUN,
		PAUSE,
		STOP;
	}
	
	private void setAnalysisMode(AnalysisMode mode) {
		switch(mode) {
		case RUN:
			// Set-up background processing.
			if (processingTask == null) {
				processingTaskTimer = new Timer();
				processingTask = new VideoProcessingTask(PlayVideoActivity.this, surfaceView);
				processingTaskTimer.schedule(processingTask, 500, VideoProcessingTask.PROCESSING_INTERVAL_MS);
			}
			processingTask.setRunning();
			break;
		case PAUSE:
			if (processingTask != null) {
				// TODO pause so we dont run analysis while paused.
				processingTask.setPaused();
			}
			break;
		case STOP:
			if (processingTask != null) {
				processingTask.cancel();
				processingTaskTimer.cancel();
				
				processingTask = null;
				processingTaskTimer = null;
			}
			break;
		}
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
			Log.i(TAG, "Player resources released");
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
		
		if (t instanceof FileNotFoundException) {
			new AlertDialog.Builder(this)
		    	.setTitle("Input Error")
		    	.setMessage("The video cannot be played.\n\n"
		    			+ "DASH streaming may not be supported for this video.")
		    	.show();
			return;
		}
		
		String msg = t.toString();
		if (msg.length() > 200) {
			msg = msg.substring(200) + "...";
		}
		
		new AlertDialog.Builder(this)
	    	.setTitle(t.getClass().getSimpleName())
	    	.setMessage(msg)
	    	.show();
	}
	
	/**
	 * Update debug text area if it is shown.
	 * @param text The new text to display.
	 */
	public void updateDebugText(String text) {
		if (this.debugText != null) {
			this.debugText.setText(text);
		}
	}
	
	// ExoPlayer.Listener
	
	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		Log.i(TAG, "onPlayerStateChanged -> " + playbackState);
		switch (playbackState) {
		case ExoPlayer.STATE_IDLE:
			break;
		case ExoPlayer.STATE_PREPARING:
			break;
		case ExoPlayer.STATE_BUFFERING:
			// Probably waiting for data to come in on the stream.
			setAnalysisMode(AnalysisMode.PAUSE);
			break;
		case ExoPlayer.STATE_READY:
			if (playWhenReady) {
				// Starting to play.
				setAnalysisMode(AnalysisMode.RUN);
			} else {
				// Paused.
				setAnalysisMode(AnalysisMode.PAUSE);
			}
			break;
		case ExoPlayer.STATE_ENDED:
			setAnalysisMode(AnalysisMode.PAUSE);
			showControls();
			break;
		default:
			break;
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
}
