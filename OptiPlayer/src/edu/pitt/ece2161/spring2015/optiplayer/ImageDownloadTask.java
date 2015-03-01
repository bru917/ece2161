package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 * This task offloads video thumbnail downloading from the search task.
 * 
 * @author Brian Rupert
 *
 */
public class ImageDownloadTask extends AsyncTask<Map<VideoProperties, String>, Void, Void> {
	
	private MainActivity ctx;
	
	ImageDownloadTask(MainActivity ctx) {
		this.ctx = ctx;
	}

	@Override
	protected Void doInBackground(Map<VideoProperties, String>... params) {
		
		Map<VideoProperties, String> map = params[0];
		
		for (Map.Entry<VideoProperties, String> entry : map.entrySet()) {
			
			String thumbUrl = entry.getValue();
			if (thumbUrl == null) {
				// Skip if no thumbnail is set.
				continue;
			}
			
			try {
				Bitmap img = downloadImage(thumbUrl);
				entry.getKey().setThumbnail(img);
				
				this.publishProgress();
				
			} catch (IOException e) {
				Log.e("bjr", "Image download failed - " + e);
			}
		}
		return null;
	}
	
	@Override
	protected void onProgressUpdate(Void... v) {
		super.onProgressUpdate(v);
		ctx.updateListView();
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
	}

	/**
	 * Downloads an image from a URL.
	 * @param urlString The URL to download from.
	 * @return A new bitmap of the image.
	 * @throws IOException On connection errors. 
	 */
	private Bitmap downloadImage(String urlString) throws IOException {
		java.net.URL url = new java.net.URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        Bitmap imageBitmap = BitmapFactory.decodeStream(conn.getInputStream());
        return imageBitmap;
	}

}
