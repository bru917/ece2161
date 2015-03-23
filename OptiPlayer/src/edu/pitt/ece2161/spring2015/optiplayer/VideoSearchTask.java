package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.ThumbnailDetails;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * This task will search the youtube API for keywords and return a list of results.
 * 
 * @author Brian Rupert
 */
public class VideoSearchTask extends AsyncTask<String, Integer, List<VideoProperties>> {

	/** Used for logging. */
	private static final String TAG = "VideoSearchTask";

	private static final String YT_API_KEY_IP = "AIzaSyD_I87Aqzsqk3Logv16THvgWaDZhIvgx1Y";
	private static final String YT_API_KEY_APPSIG = "AIzaSyDorMD0uUuD4FS7p8E75InUIDNovijgvOM";

	/**
	 * Define a global instance of the HTTP transport.
	 */
	public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/**
	 * Define a global instance of the JSON factory.
	 */
	public static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private int maxResults = 10;

	private Context ctx;

	private Exception exception;

	/**
	 * Constructor.
	 * 
	 * @param ctx
	 *            A context to run in.
	 */
	VideoSearchTask(Context ctx) {
		this.ctx = ctx;
	}

	@Override
	protected List<VideoProperties> doInBackground(String... params) {
		// First parameter should be query search terms.
		String searchTerms = params[0];

		try {
			YouTube youtube;

			HttpRequestInitializer init = new HttpRequestInitializer() {
				public void initialize(HttpRequest request) throws IOException {

				}
			};

			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, init)
					.setApplicationName(ctx.getString(R.string.app_name))
					.build();
			
			YouTube.Search.List search = youtube.search().list("id,snippet");

			search.setKey(this.getApiKey());
			search.setQ(searchTerms);
			search.setType("video");
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
			search.setMaxResults((long) maxResults);

			publishProgress(1);

			// Call the API and print results.
			SearchListResponse searchResponse = search.execute();

			publishProgress(2);

			List<SearchResult> searchResultList = searchResponse.getItems();

			return toProps(searchResultList);
		} catch (GoogleJsonResponseException e) {
			this.exception = e;
			Log.w(TAG, "YT error (" + e.getDetails().getCode() + ") "
					+ e.getDetails().getMessage());
		} catch (IOException e) {
			this.exception = e;
			Log.e(TAG, "IO error - " + e);
		}

		return null;
	}

	/**
	 * Gets the appropriate API key for the type of connection being used.
	 * @return
	 */
	private String getApiKey() {
		ConnectivityManager connectivityManager = (ConnectivityManager) this.ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		String key = null;
		if (mWifi.isConnected() || "unknown".equalsIgnoreCase(Build.MANUFACTURER)) {
			key = YT_API_KEY_IP;
			Log.d(TAG, "Using IP-based API key.");
		} else {
			key = YT_API_KEY_APPSIG;
			Log.d(TAG, "Using app signature-based API key.");
		}
		return key;
	}

	/**
	 * Converts a list of youtube search results into a list of video data
	 * objects for use in this application.
	 * 
	 * @param results
	 * @return
	 */
	private List<VideoProperties> toProps(List<SearchResult> results) {

		if (results == null) {
			// No results to process.
			return Collections.emptyList();
		}

		List<VideoProperties> newlist = new ArrayList<VideoProperties>();

		int i = 1;

		Map<VideoProperties, String> imageUrlMap = new LinkedHashMap<VideoProperties, String>();

		for (SearchResult r : results) {
			VideoProperties info = new VideoProperties();
			// Assign the video ID.
			info.setVideoId(r.getId().getVideoId());
			// Assign the video's title text.
			info.setTitle(r.getSnippet().getTitle());

			info.setUrl("http://www.youtube.com/watch?v="
					+ r.getId().getVideoId());

			// Inspect thumbnail properties and download the image.
			ThumbnailDetails d = r.getSnippet().getThumbnails();
			if (d != null) {
				String thumbUrl = null;
				// Search for high, then medium, then default.
				if (d.getHigh() != null) {
					thumbUrl = d.getHigh().getUrl();
				} else if (d.getMedium() != null) {
					thumbUrl = d.getMedium().getUrl();
				} else if (d.getDefault() != null) {
					thumbUrl = d.getDefault().getUrl();
				}

				if (thumbUrl != null) {
					imageUrlMap.put(info, thumbUrl);
				}
			}
			newlist.add(info);

			this.publishProgress(2 + i);
			i++;
		}

		ImageDownloadTask imgDownloader = new ImageDownloadTask((MainActivity) ctx);
		imgDownloader.execute(imageUrlMap);

		return newlist;
	}

	/**
	 * Gets the maximum expected value for progress indication.
	 * 
	 * @return
	 */
	public int getProgressMax() {
		return 2 + this.maxResults;
	}

	public Exception getException() {
		return this.exception;
	}
}
