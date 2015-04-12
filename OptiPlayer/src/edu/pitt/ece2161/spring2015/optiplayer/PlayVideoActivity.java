package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.google.android.exoplayer.drm.MediaDrmCallback;

import edu.pitt.ece2161.spring2015.optiplayer.ServerCommunicator.CommCallback;
import edu.pitt.ece2161.spring2015.optiplayer.ServerCommunicator.CommStatus;
import edu.pitt.ece2161.spring2015.optiplayer.VideoAnalysisDataset.DataEntry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

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
	public static final String VIDEO_TITLE = "VideoTitle";
	public static final String VIDEO_URL = "VideoURL";
	public static final String LOCAL_VIDEO_PATH = "LocalVideoPath";
	
	private static final String STATE_PLAYER_POSITION = "PLAYER_POSITION";
	private static final String STATE_ORIG_BACKLIGHT = "STATE_ORIG_BACKLIGHT";
	private static final String STATE_DASH_MANIFEST = "STATE_DASH_MANIFEST";

	private CustomView surfaceView;
	private MediaController mediaController;

	private CustomPlayer player;
	private boolean playerNeedsPrepare;
	private long playerPosition = 0;
	private boolean enableBackgroundAudio;

	private VideoProperties videoProperties;
	private String localVideoFile;
	
	private TextView debugText;
	
	private Mode mode = null;
	private VideoBackgroundTask backgroundTask;
	private Timer backgroundTaskTimer;
	
	private Integer origBrightnessSetting;
	private String dashManifestUrl;
	
	private enum Mode {
		Analysis,
		Playback
	}
	
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
		
		// Do not display the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_play_video);
		
		Log.i(TAG, "Activity is being CREATED");
		
		// Remember the original brightness setting, to restore it later.
		int bl = -1;
		if (savedInstanceState != null) {
			bl = savedInstanceState.getInt(STATE_ORIG_BACKLIGHT, -1);
		}
		if (bl > 0) {
			origBrightnessSetting = bl;
		} else {
			try {
				origBrightnessSetting = Settings.System.getInt(
						getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
				Log.d(TAG, "Saved original brightness level of " + this.origBrightnessSetting);
			} catch (SettingNotFoundException e) {
				origBrightnessSetting = null;
			}
		}
		
		if (savedInstanceState != null) {
			// Check if there is a player position due to activity restart
			// while playing the video
			long pos = savedInstanceState.getLong(STATE_PLAYER_POSITION, -1L);
			if (pos > 0) {
				this.playerPosition = pos;
			}
			
			this.dashManifestUrl = savedInstanceState.getString(STATE_DASH_MANIFEST, null);
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
		
		if (AppSettings.getInstance().isDebugMode()) {
			debugText = (TextView) this.findViewById(R.id.tDebugText);
			debugText.setVisibility(View.VISIBLE);
		}

		// Pull the video ID that should be streamed.
		this.videoProperties = getVideoProperties(this.getIntent().getExtras());
		
		this.localVideoFile = this.getIntent().getExtras().getString(LOCAL_VIDEO_PATH);
		// Without a youtube video, there is nothing to wait for or download.
		if (this.videoProperties != null) {
			// Check for local file:
			final File localFile = AppSettings.getInstance()
					.getLocalAnalysisFile(this.videoProperties.getVideoId());
			
			Log.d(TAG, "Checking for cached file " + localFile);
			if (localFile.exists()) {
				Log.d(TAG, "Found cached file");
				
				if (AppSettings.getInstance().isDebugMode() && savedInstanceState == null) {
					// Begin playback on dismissal of the dialog.
					OnDismissListener odl = new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							beginPlayingVideo(localFile, null);
						}
					};
					
					new AlertDialog.Builder(PlayVideoActivity.this)
						.setTitle("Dimming Information")
						.setMessage("Reading locally cached dimming file")
						.setCancelable(false)
						.setPositiveButton("Play", null)
						.setOnDismissListener(odl)
						.show();
				} else {
					beginPlayingVideo(localFile, null);
				}
				
			} else {
				// Ask server for file:
				requestDimmingFile(this.videoProperties.getVideoId());
			}
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
				
				if (isPlaybackMode()) {
					// When in playback mode, we can immediately begin to play
					setPlayWhenReady();
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
				//Log.v(TAG, "onSurfaceTextureUpdated -> " + surface);
			}
		});
		Log.i(TAG, "Activity creation complete");
	}

	private VideoProperties getVideoProperties(Bundle extras) {
		String vidId = extras.getString(VIDEO_ID);
		if (vidId == null || vidId.isEmpty()) {
			return null;
		}
		VideoProperties props = new VideoProperties();
		props.setVideoId(vidId);
		props.setTitle(extras.getString(VIDEO_TITLE));
		props.setUrl(extras.getString(VIDEO_URL));
		return props;
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG, "Activity state is being saved");
		outState.putLong(STATE_PLAYER_POSITION, this.playerPosition);
		outState.putInt(STATE_ORIG_BACKLIGHT, this.origBrightnessSetting);
		outState.putString(STATE_DASH_MANIFEST, this.dashManifestUrl);
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
		
		setBackgroundTaskState(BackgroundTaskState.PAUSE);
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
		
		setBackgroundTaskState(BackgroundTaskState.STOP);
	}
	
	@Override
	public void onConfigurationChanged(Configuration cfg) {
		super.onConfigurationChanged(cfg);
		// DOESNT WORK??
		Log.i(TAG, "Configuration changed -> " + cfg);
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
		//progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setProgress(0);
		progress.show();
		// Set up a request to the dimming server.
		final ServerCommunicator comm = new ServerCommunicator();
		final RequestFileCallback cb = new RequestFileCallback(progress);
		// Request any existing dimming information.
		Log.d(TAG, "Beginning download request");
		comm.download(PlayVideoActivity.this, videoProperties, cb);
	}
	
	private class RequestFileCallback implements CommCallback {
		
		private ProgressDialog progress;
		
		RequestFileCallback(ProgressDialog progress) {
			this.progress = progress;
		}
		
		@Override
		public void execute(final CommStatus statusCode, final Object data) {
			
			Runnable r = new Runnable() {
				public void run() {
					String filePath = null;
					String msg = statusCode.toString();
					if (statusCode == CommStatus.DownloadNotFound) {
						msg = "A dimming file does not exist yet";
					} else if (statusCode == CommStatus.DownloadOk) {
						msg = "Dimming file was downloaded";
						filePath = (String) data;
					}
					
					File f = null;
					if (filePath != null) {
						f = new File(filePath);
					}
					final File ff = f;
					
					if (!AppSettings.getInstance().isDebugMode()
							&& statusCode == CommStatus.DownloadNotFound
							|| statusCode == CommStatus.DownloadOk) {
						// Not in debug and not an error code, so just go...
						beginPlayingVideo(ff, RequestFileCallback.this.progress);
						return;
					}
					
					OnDismissListener odl = new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							beginPlayingVideo(ff, RequestFileCallback.this.progress);
						}
					};
					
					new AlertDialog.Builder(PlayVideoActivity.this)
						.setTitle("Dimming Information")
						.setMessage(msg)
						.setCancelable(false)
						.setPositiveButton("Play", null)
						.setOnDismissListener(odl)
						.show();
				}
			};
			
			PlayVideoActivity.this.runOnUiThread(r);
		}
	}
	
	private void beginPlayingVideo(final File dimmingFile, ProgressDialog progress) {
		Log.d(TAG, "beginPlayingVideo");
		
		if (progress == null) {
			ProgressDialog d = new ProgressDialog(PlayVideoActivity.this);
			d.setTitle("Preparing");
			d.setMessage("Please Wait...");
			d.setIndeterminate(true);
			d.show();
			progress = d;
		}
		
		final ProgressDialog prog = progress;
		
		Thread t = new Thread() {
			public void run() {
				if (dimmingFile != null) {
					if (!prepareFilePlayback(dimmingFile)) {
						// File parsing error.
						mode = Mode.Analysis;
					} else {
						// Dimming file is prepared and ready.
						mode = Mode.Playback;
					}
				} else {
					// No dimming file yet.
					mode = Mode.Analysis;
				}
				setPlayWhenReady();
				if (prog != null) {
					prog.dismiss();
				}
			}
		};
		t.start();
	}
	
	private void setPlayWhenReady() {
		if (isAnalysisMode()) {
			// In analysis mode... do not allow changes to orientation.
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
		if (player != null) {
			player.setPlayWhenReady(true);
		}
	}

	/**
	 * Begins preparing the video player resources.
	 */
	private void preparePlayer() {
		String videoId = null;
		if (this.videoProperties != null) {
			videoId = this.videoProperties.getVideoId();
		}

		if (player == null) {
			MediaDrmCallback drmCallback = new MyDrmCallback();

			ExoRendererBuilder rb;
			if (videoId == null) {
				// Play local video file:
				rb = new ExoDefaultRendererBuilder(this, Uri.parse(this.localVideoFile));
				
			} else {
				// Play video using DASH streaming:
				if (dashManifestUrl == null) {
					dashManifestUrl = VideoUrlProcessor.getDashUrl(this, videoId);
				}
				rb = new ExoDashRendererBuilder(getUserAgent(this), dashManifestUrl, videoId, drmCallback, null);
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
		// PlayWhenReady is set after we get our dimming file response.
		// We will set up either in analysis mode or replay mode.
		player.setPlayWhenReady(false);
	}
	
	public enum BackgroundTaskState {
		RUN,
		PAUSE,
		STOP;
	}
	
	private void setBackgroundTaskState(BackgroundTaskState state) {
		switch(state) {
		case RUN:
			// Set-up background processing.
			if (backgroundTaskTimer == null) {
				backgroundTaskTimer = new Timer();
				if (isAnalysisMode()) {
					// Prepare the video processing task.
					VideoProcessingTask t = new VideoProcessingTask(PlayVideoActivity.this, surfaceView);
					backgroundTask = t;
					
					backgroundTaskTimer.schedule(t, 100, t.getCycleTime());
				} else if (isPlaybackMode()) {
					// Should have already been prepared for us...
					FrameAnalysisPlayback t = (FrameAnalysisPlayback) backgroundTask;
					
					backgroundTaskTimer.schedule(t, 100, t.getCycleTime());
				} else {
					throw new IllegalStateException("oops");
				}
			}
			backgroundTask.setRunning();
			break;
		case PAUSE:
			if (backgroundTaskTimer != null) {
				backgroundTask.setPaused();
			}
			break;
		case STOP:
			if (backgroundTaskTimer != null) {
				backgroundTask.cancel();
				backgroundTaskTimer.cancel();
				
				backgroundTask = null;
				backgroundTaskTimer = null;
			}
			break;
		}
	}
	
	/**
	 * Store video analysis data to a file.
	 * @param data
	 */
	private void saveResults(final VideoAnalysisDataset data, final VideoProperties props) {
		//printToLog(data);
		
		final SaveAnalysisTask task = new SaveAnalysisTask(this, data);
		
		
		Thread t = new Thread() {
			public void run() {
				File file = null;
				try {
					file = task.get();
				} catch (InterruptedException e) {
					Log.e(TAG, "SaveAnalysisTask Error " + e);
				} catch (ExecutionException e) {
					Log.e(TAG, "SaveAnalysisTask Error " + e);
				}
				
				if (file == null) {
					// File save failed.
					
					
					// Do not try to push to the server.
					return;
				}
				
				if (props != null) {
					final String filePath = file.getAbsolutePath();
					
					final CommCallback cb = new CommCallback() {
						@Override
						public void execute(CommStatus status, Object data) {
							new AlertDialog.Builder(PlayVideoActivity.this)
								.setTitle("Upload Result")
								.setMessage(String.valueOf(status)
										+ "\n\nServer Response: " + data
										+ "\n\nThank you!")
								.show();
						}
					};
					
					// Set up a request to the dimming server.
					Thread serverCommThread = new Thread() {
						public void run() {
							ServerCommunicator comm = new ServerCommunicator();
							comm.upload(PlayVideoActivity.this, filePath, props, cb);
						}
					};
					runOnUiThread(serverCommThread);
				}
			}
		};
		
		task.execute();
		t.start();
	}
	
	/**
	 * Writes the data set to the debug log.
	 * @param data The data to write.
	 */
	protected static void printToLog(VideoAnalysisDataset data) {
		Log.v(TAG, "======  <RESULTS>  ======");
		int i = 0;
		for (DataEntry d : data.getDataList()) {
			Log.v(TAG, "  DATA[" + i + "] -> position=" + d.getPosition() + " level=" + d.getLevel());
			i++;
		}
		Log.v(TAG, "======  </RESULTS>  ======");
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
	
	private boolean isAnalysisMode() {
		return this.mode == Mode.Analysis;
	}
	
	private boolean isPlaybackMode() {
		return this.mode == Mode.Playback;
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
			setBackgroundTaskState(BackgroundTaskState.PAUSE);
			break;
		case ExoPlayer.STATE_READY:
			if (playWhenReady) {
				// Starting to play.
				setBackgroundTaskState(BackgroundTaskState.RUN);
			} else {
				// Paused.
				setBackgroundTaskState(BackgroundTaskState.PAUSE);
			}
			break;
		case ExoPlayer.STATE_ENDED:
			setBackgroundTaskState(BackgroundTaskState.PAUSE);
			
			if (backgroundTask instanceof VideoProcessingTask) {
				// Done playing...
				VideoProcessingTask t = (VideoProcessingTask) backgroundTask;
				VideoAnalysisDataset data = t.getDataset();
				data.setVideoId(this.videoProperties.getVideoId());
				
				// Fill in properties that would not have been set yet.
				this.videoProperties.setLength(this.player.getDuration());
				this.videoProperties.setOptDimLv("1");
				this.videoProperties.setOptStepLgh(String.valueOf(backgroundTask.getCycleTime()));
				
				saveResults(data, this.videoProperties);
			}
			
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
	
	private boolean prepareFilePlayback(File localFile) {
		VideoAnalysisFileHandler fh = new VideoAnalysisFileHandler();
		VideoAnalysisDataset data = null;
		try {
			data = fh.readFlatFile(localFile);
		} catch (Exception e) {
			Log.e(TAG, "Error reading file: " + e);
			Toast.makeText(this, "Error Reading Cache", Toast.LENGTH_SHORT).show();
			// Try to delete the bogus file...
			localFile.delete();
			// Change to analysis mode.
			return false;
		}
		
		backgroundTask = new FrameAnalysisPlayback(this, data);
		return true;
	}

	public CustomPlayer getPlayer() {
		return this.player;
	}
	
	public CustomView getPlayerView() {
		return this.surfaceView;
	}
}
