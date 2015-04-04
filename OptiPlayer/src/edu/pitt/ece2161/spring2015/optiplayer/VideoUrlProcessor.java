package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * This class figures out the appropriate URL/URI to display the video content.
 * 
 * @author Brian Rupert
 *
 */
final class VideoUrlProcessor {
	
	private static final String TAG = "VideoUrlProcessor";

	/**
	 * Gets a URL to a DASH manifest.
	 * @param ctx The current context.
	 * @param videoId The requested video ID.
	 * @return The URL to the DASH manifest.
	 */
	public static String getDashUrl(Context ctx, String videoId) {
		// Produce DASH manifest from a video ID.

		final String url = "https://www.youtube.com/watch?v=" + videoId;
		
		AsyncTask<Void, Void, String> t = new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
		        HttpClient client = new DefaultHttpClient();
		        HttpResponse response;
		        String responseString = null;
		        try {
		        	// Fetch the content at the URL
		            response = client.execute(new HttpGet(url));
		            StatusLine statusLine = response.getStatusLine();
		            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
		                ByteArrayOutputStream out = new ByteArrayOutputStream();
		                response.getEntity().writeTo(out);
		                responseString = out.toString();
		                out.close();
		            } else {
		                // Close the connection.
		                response.getEntity().getContent().close();
		                throw new IOException(statusLine.getReasonPhrase());
		            }
		        } catch (Exception e) {
		            Log.e(TAG, "Error while fetching Youtube DASH manifest URL: " + e);
		        }
		        
		        Log.v(TAG, "Inspecting page " + url);
		        
		        // Process the content for a DASH manifest URL
		        int dashStart = responseString.indexOf("\"" + DASH_KEY + "\"");
		        if (dashStart > 0) {
		        	
		        	// Used for debugging the DASH URL parsing.
		        	/*
		        	String dashDataStart = responseString.substring(dashStart);
		        	if (dashDataStart.length() > 3000) {
		        		dashDataStart = dashDataStart.substring(0, 3000);
		        	}
		        	*/
		        	
		        	// The DASH URL is contained in a javascript/json formatted
		        	// string, so either of these sequences may delimit the end
		        	// of the URL we're trying to extract:
		        	int dashEnd = responseString.indexOf("\",", dashStart);
		        	int dashEnd2 = responseString.indexOf("\"},", dashStart);
		        	
		        	if (dashEnd2 > 0 && dashEnd2 < dashEnd) {
		        		dashEnd = dashEnd2;
		        	}
		        	
		        	String dashUrl = responseString.substring(dashStart, dashEnd);
		        	// Strip off the DASH property key.
		        	int urlBegin = dashUrl.indexOf("http:");
		        	dashUrl = dashUrl.substring(urlBegin);
		        	// Replace the raw escape sequences for backslashes.
		        	dashUrl = dashUrl.replace("\\/", "/");
		        	
		        	responseString = dashUrl;
		        } else {
		        	Log.e(TAG, "Couldn't find DASH manifest link at " + url);
		        	responseString = null;
		        }
		        
				return responseString;
			}
			
		};
		t.execute((Void[]) null);
		String dashUrl = GOOGLE_GLASS_COMMERCIAL;
		try {
			dashUrl = t.get();
		} catch (Exception e) {
			Log.e(TAG, "Async task error: " + e);
		}
		
		try {
			Uri dashUri = Uri.parse(dashUrl);
			return dashUri.toString();
		} catch (Exception e) {
			Log.e(TAG, "URI parser error: " + e + " for URL '" + dashUrl + "'");
		}
		
		return null;
	}
	
	private static final String DASH_KEY = "dashmpd";
	
	// A known working DASH manifest URL.
	private static final String GOOGLE_GLASS_COMMERCIAL =
			"http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube"
			+ "?as=fmp4_audio_clear,fmp4_sd_hd_clear"
			+ "&sparams=ip,ipbits,expire,as"
			+ "&ip=0.0.0.0"
			+ "&ipbits=0"
			+ "&expire=19000000000"
			+ "&signature=255F6B3C07C753C88708C07EA31B7A1A10703C8D.2D6A28B21F921D0B245CDCF36F7EB54A2B5ABFC2"
			+ "&key=ik0";

}
