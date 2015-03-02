package edu.pitt.ece2161.spring2015.optiplayer;

import com.google.android.exoplayer.VideoSurfaceView;

import edu.pitt.ece2161.spring2015.optiplayer.ServerCommunicator.CommCallback;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;

/**
 * This activity is used to play the video content.
 * 
 * @author Brian Rupert
 *
 */
public class PlayVideoActivity extends Activity {

	private ProgressDialog progress;

	private VideoSurfaceView surfaceView;
	private MediaController mediaController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play_video);

		progress = new ProgressDialog(this);
		progress.setTitle("Please wait");
		progress.setMessage("Loading video...");
		progress.setIndeterminate(true);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setProgress(0);
		progress.show();

		String videoId = this.getIntent().getExtras().getString("VideoID");
		
		ServerCommunicator comm = new ServerCommunicator();
		CommCallback cb = new CommCallback() {
			public void execute(ServerCommunicator.CommStatus statusCode, Object data) { 
				
                new AlertDialog.Builder(PlayVideoActivity.this)
	            	.setTitle("Dimming Information")
	            	.setMessage(statusCode + "")
	                .setPositiveButton("OK", null)
	                .show();
				
				progress.dismiss();
			}
		};
		comm.download(this, videoId, cb);

		
		//LoadVideoTask task = new LoadVideoTask();
		//task.execute(videoId);

		mediaController = new MediaController(this);

		surfaceView = (VideoSurfaceView) findViewById(R.id.vSurfaceView);
	}

	private CustomPlayer player;
	private boolean playerNeedsPrepare;

	private void preparePlayer() {
		if (player == null) {
			player = new CustomPlayer();
			// player.addListener(this);
			// player.setTextListener(this);
			// player.setMetadataListener(this);
			player.seekTo(0);
			playerNeedsPrepare = true;
			mediaController.setMediaPlayer(player.getPlayerControl());
			mediaController.setEnabled(true);
			// eventLogger = new EventLogger();
			// eventLogger.startSession();
			// player.addListener(eventLogger);
			// player.setInfoListener(eventLogger);
			// player.setInternalErrorListener(eventLogger);
		}
		if (playerNeedsPrepare) {
			player.prepare();
			playerNeedsPrepare = false;
			//updateButtonVisibilities();
		}
		player.setSurface(surfaceView.getHolder().getSurface());
		player.setPlayWhenReady(true);
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

//	/**
//	 * This asynchronous task can fetch the dimming scheme and load the video
//	 * file/stream.
//	 * 
//	 * @author Brian Rupert
//	 *
//	 */
//	private class LoadVideoTask extends AsyncTask<String, Void, Void> {
//
//		@Override
//		protected Void doInBackground(String... id) {
//			// Perform video loading...
//			
//			String videoId = id[0];
//			
//			try {
//				Thread.sleep(3000);
//			} catch (InterruptedException e) { }
//
//			//ServerCommunicator comm = new ServerCommunicator();
//			//comm.download(PlayVideoActivity.this, videoId);
//
//			return null;
//		}
//
//		@Override
//		protected void onProgressUpdate(Void... values) {
//			// Update progress indicator.
//			progress.incrementProgressBy(1);
//		}
//
//		@Override
//		protected void onPostExecute(Void result) {
//			// Called when complete.
//			progress.dismiss();
//		}
//	}

}
