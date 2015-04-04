package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.ArrayList;
import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * The main user interface activity invoked on application start-up.
 * 
 * @author Brian Rupert
 *
 */
public class MainActivity extends Activity implements OnItemClickListener {
	
	private VideoResultsListAdapter listAdapter;
	
	// Defined as static so the state is saved - not sure if thats a cheap trick.
	private static List<VideoProperties> searchResultList = new ArrayList<VideoProperties>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		listAdapter = new VideoResultsListAdapter();
		
		ListView listView = (ListView) findViewById(R.id.searchList);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);
		
		TextView searchInput = (TextView) findViewById(R.id.searchText);
		searchInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				clickSearchButton(v);
				return true;
			}
		});
		
		if (AppSettings.DEBUG) {
			// Insert text for quick debugging
			searchInput.setText("clarkson bmw estate");
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
	 * Invoked when the search "Go" button is pressed.
	 * @param view A reference to the button.
	 */
	public void clickSearchButton(View view) {
		executeVideoSearch();
		//playYoutubeVideo(null);
	}
	
	/**
	 * Invokes the player using the specified YouTube video ID.
	 * If the ID is null, a video player "test mode" uses a local file.
	 * @param videoId The video ID.
	 */
	private void playYoutubeVideo(VideoProperties props) {
		// Invoked when a video in the search list is clicked.
		Intent i = new Intent(MainActivity.this, PlayVideoActivity.class);
		if (props == null) {
			// Use a local file to test.
			i.putExtra(PlayVideoActivity.LOCAL_VIDEO_PATH,
					Environment.getExternalStorageDirectory().getPath() + "/test_video.mp4");
		} else {
			i.putExtra(PlayVideoActivity.VIDEO_ID, props.getVideoId());
			i.putExtra(PlayVideoActivity.VIDEO_TITLE, props.getTitle());
			i.putExtra(PlayVideoActivity.VIDEO_URL, props.getUrl());
		}
		startActivity(i);
	}
	
	/**
	 * Performs the youtube search based on the user text input.
	 */
	private void executeVideoSearch() {
		TextView txt = (TextView) findViewById(R.id.searchText);
		String queryTerm = txt.getText().toString();
		
		final ProgressDialog progress = new ProgressDialog(this);
		final VideoSearchTask task = new VideoSearchTask(this) {
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
			
			@Override
			protected void onPostExecute(List<VideoProperties> results) {
				super.onPostExecute(results);
				
				progress.dismiss();
				
				if (this.getException() != null) {
					Exception e = this.getException();
					String msg = e.getMessage();
					if (e instanceof GoogleJsonResponseException) {
						GoogleJsonResponseException gx = (GoogleJsonResponseException) e;
						msg = "Error " + gx.getDetails().getCode()
								+ ": " + gx.getDetails().getMessage();
					}
					
					new AlertDialog.Builder(MainActivity.this)
		            	.setTitle("YouTube Service Error")
		            	.setMessage(msg)
		            	.show();
				} else {
					// Clear-out the old results.
					searchResultList.clear();
					// Add all the new results.
					searchResultList.addAll(results);
					// Tell the list view to update.
					listAdapter.notifyDataSetChanged();
				}
			}
			
			@Override
			protected void onCancelled(List<VideoProperties> results) {
				// Handle cancellation if needed here.
			}
			
			@Override
			protected void onProgressUpdate(Integer... values) {
				if (values != null && values.length > 0) {
					progress.setProgress(values[0]);
				}
			}
		};
		progress.setTitle("Searching");
		progress.setIndeterminate(false);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setCancelable(true);
		progress.setMax(task.getProgressMax());
		progress.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// User pressed 'cancel' in the progress dialog.
				task.cancel(true);
			}
		});
		progress.show();
		
		task.execute(queryTerm);
		
		// Hide keyboard control to better display video results.
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// Get the properties element from the clicked item in the list.
		VideoProperties vidProps = (VideoProperties) parent.getItemAtPosition(position);
		// Pass the video ID to the player and begin playing.
		playYoutubeVideo(vidProps);
	}
	
	/**
	 * This adapter controls the display of the video list.
	 * 
	 * @author Brian Rupert
	 *
	 */
	private class VideoResultsListAdapter extends BaseAdapter implements ListAdapter {
		
		@Override
		public int getCount() {
			return searchResultList.size();
		}

		@Override
		public Object getItem(int position) {
			return searchResultList.get(position);
		}

		@Override
		public long getItemId(int position) {
			VideoProperties info = (VideoProperties) getItem(position);
			if (info != null) {
				return info.hashCode();
			}
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final VideoProperties info = (VideoProperties) this.getItem(position);
			
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.video_list_item, parent, false);
			}
			
			TextView titleText = (TextView) convertView.findViewById(R.id.resultTitle);
			titleText.setText(info.getTitle());
			
			TextView descText = (TextView) convertView.findViewById(R.id.resultDescription);
			descText.setText(null);
			
			TextView lengthText = (TextView) convertView.findViewById(R.id.videoLength);
			lengthText.setText(formatDuration(info.getLength()));
			
			final ImageView img = (ImageView) convertView.findViewById(R.id.resultImage);
			if (info.getThumbnail() != null) {
				img.setImageBitmap(info.getThumbnail());
			} else {
				img.setImageBitmap(
						BitmapFactory.decodeResource(getResources(), R.drawable.placeholder));
			}
			
			return convertView;
		}
		
	}
	
	private static String formatDuration(Long duration) {
		if (duration == null || duration == 0) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		long seconds = duration % 60;
		long minutes = duration / 60;
		long hours = duration / 3600;
	
		if (hours > 0) {
			sb.append(hours);
			sb.append(":");
			if (minutes == 0) {
				sb.append("00");
			} else if (minutes < 10) {
				sb.append("0");
			}
		}
		sb.append(String.valueOf(minutes));
		sb.append(":");
		if (seconds < 10) {
			sb.append("0");
		}
		sb.append(String.valueOf(seconds));
		
		return sb.toString();
	}

	public void updateListView() {
		this.listAdapter.notifyDataSetChanged();
	}
}
