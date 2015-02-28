package edu.pitt.ece2161.spring2015.optiplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This activity is used to play the video content.
 * 
 * @author Brian Rupert
 *
 */
public class PlayVideoActivity extends Activity {
	
	private ProgressDialog progress;

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
		
		LoadVideoTask task = new LoadVideoTask();
		task.execute();
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
	 * This asynchronous task can fetch the dimming scheme and load the
	 * video file/stream.
	 * 
	 * @author Brian Rupert
	 *
	 */
	private class LoadVideoTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			// Perform video loading...
			
			// Note: sleeps are here for testing only.
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
			
			progress.setIndeterminate(false);
			progress.setMax(8);
			
			for (int i = 0; i < 8; i++) {
				publishProgress((Void) null);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					
				}
			}

			return null;
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			// Update progress indicator.
			progress.incrementProgressBy(1);
	    }
		
		@Override
		protected void onPostExecute(Void result) {
			// Called when complete.
			progress.dismiss();
		}
	}

}
