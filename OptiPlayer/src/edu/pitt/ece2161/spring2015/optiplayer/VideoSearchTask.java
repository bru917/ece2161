package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 * This task will search the youtube API for keywords and return a list of
 * results.
 * 
 * @author Brian Rupert
 */
public class VideoSearchTask extends AsyncTask<String, Void, List<VideoProperties>> {
	
	private static final String YT_API_KEY = "AIzaSyD_I87Aqzsqk3Logv16THvgWaDZhIvgx1Y";
	
    /**
     * Define a global instance of the HTTP transport.
     */
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Define a global instance of the JSON factory.
     */
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();
    
    private Context ctx;
	
    VideoSearchTask(Context ctx) {
    	this.ctx = ctx;
    }

	@Override
	protected List<VideoProperties> doInBackground(String... params) {
		
		String queryTerm = params[0];
		
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

            search.setKey(YT_API_KEY);
            search.setQ(queryTerm);
            search.setType("video");
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults((long) 10);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            
            return toProps(searchResultList);
        } catch (GoogleJsonResponseException e) {
        	
            Log.w("bjr", "YT error (" + e.getDetails().getCode() + ") "
                    + e.getDetails().getMessage());
            
            new AlertDialog.Builder(ctx)
            	.setTitle("YouTube Service Error")
            	.setMessage(e.getDetails().getCode() + ": " + e.getDetails().getMessage())
            	.show();

        } catch (Throwable t) {
            t.printStackTrace();
        }
        
		return null;
	}
	
	private List<VideoProperties> toProps(List<SearchResult> results) {
		
		List<VideoProperties> newlist = new ArrayList<VideoProperties>();

		if (results != null) {
			for (SearchResult r : results) {
				VideoProperties info = new VideoProperties();
				
				info.setVideoId(r.getId().getVideoId());
				info.setTitle(r.getSnippet().getTitle());
				
				ThumbnailDetails d = r.getSnippet().getThumbnails();
				if (d != null) {
					String thumbUrl = null;
					if (d.getHigh() != null) {
						thumbUrl = d.getHigh().getUrl();
					} else if (d.getMedium() != null) {
						thumbUrl = d.getMedium().getUrl();
					} else if (d.getDefault() != null) {
						thumbUrl = d.getDefault().getUrl();
					}
					
					if (thumbUrl != null) {
						try {
							info.setThumbnail(downloadImage(thumbUrl));
						} catch (IOException e) {
							Log.e("bjr", "" + e);
						}
					}
				}
				newlist.add(info);
			}
		}
		
		return newlist;
	}

	private Bitmap downloadImage(String urlString) throws IOException {
		java.net.URL url = new java.net.URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        InputStream input = conn.getInputStream();
        Bitmap myBitmap = BitmapFactory.decodeStream(input);
        return myBitmap;
	}
}
