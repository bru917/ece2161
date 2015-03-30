package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.File;
import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Saves the analysis file in the background.
 * 
 * @author Brian Rupert
 */
public class SaveAnalysisTask extends AsyncTask<Void, Integer, File> {
	
	private static final String TAG = "SaveAnalysisTask";
	
	private Context ctx;
	
	private VideoAnalysisDataset data;
	
	private ProgressDialog progress;
	
	SaveAnalysisTask(Context ctx, VideoAnalysisDataset data) {
		this.ctx = ctx;
		this.data = data;
	}
	
	@Override
    protected void onPreExecute() {
		super.onPreExecute();
		// Set up a progress dialog to give the user instant feedback.
		progress = new ProgressDialog(ctx);
		progress.setTitle("Please wait");
		progress.setMessage("Saving analysis...");
		progress.setIndeterminate(true);
		//progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setProgress(0);
		progress.show();
    }

	@Override
	protected File doInBackground(Void... params) {
		
		// First, save to file system.
		if (data.getVideoId() == null) {
			throw new IllegalArgumentException("Video ID is undefined");
		}
		
		try {
			// Wait for one interval for the processing to be completely done.
			// Otherwise we may miss the final frame value.
			Thread.sleep(VideoProcessingTask.PROCESSING_INTERVAL_MS);
		} catch (InterruptedException e1) {
			
		}
		
		File file = AppSettings.getInstance().getLocalAnalysisFile(data.getVideoId());
		
		VideoAnalysisFileHandler fh = new VideoAnalysisFileHandler();
		try {
			fh.writeFlatFile(file, data);
		} catch (IOException e) {
			Log.e(TAG, "Error saving file: " + e);
			return null;
		}
		Log.d(TAG, "Saved analysis file to " + file);
		
		/*
		// Used to test the dialog shows up. It wont for small files.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		return file;
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		if (values == null) {
			return;
		}
		Integer p = values[0];
		if (p != null) {
			progress.setProgress(p);
		}
	}
	
	@Override
	protected void onCancelled(File result) {
		progress.dismiss();
	}

	@Override
	protected void onPostExecute(File result) {
		progress.dismiss();
	}
}
