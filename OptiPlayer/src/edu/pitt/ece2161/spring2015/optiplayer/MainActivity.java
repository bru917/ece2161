package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The main user interface activity invoked on application start-up.
 * 
 * @author Brian Rupert
 *
 */
public class MainActivity extends Activity implements OnItemClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ListView listView = (ListView) findViewById(R.id.listView1);
		listView.setAdapter(new VideoResultsListAdapter());
		listView.setOnItemClickListener(this);
	}

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
	
	private void playVideo() {
		Intent i = new Intent(MainActivity.this, PlayVideoActivity.class);
		startActivity(i);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		playVideo();
	}
	
	/**
	 * Holds basic information for a video shown in the search list.
	 * 
	 * @author Brian Rupert
	 *
	 */
	private class VideoSummaryInfo {
		private int id;
		private String title;
		private String description;
	}
	
	/**
	 * This adapter controls the display of the video list.
	 * 
	 * @author Brian Rupert
	 *
	 */
	private class VideoResultsListAdapter extends BaseAdapter implements ListAdapter {
		
		private List<VideoSummaryInfo> delegate = new ArrayList<VideoSummaryInfo>();
		
		VideoResultsListAdapter() {
			
			for (int i = 0; i < 10; i++) {
				VideoSummaryInfo test = new VideoSummaryInfo();
				test.id = 1;
				test.title = "Test Video Title (" + i + ")";
				test.description = "A video description is shown here";
				
				this.delegate.add(test);
			}
		}

		@Override
		public int getCount() {
			return delegate.size();
		}

		@Override
		public Object getItem(int position) {
			return delegate.get(position);
		}

		@Override
		public long getItemId(int position) {
			VideoSummaryInfo info = (VideoSummaryInfo) getItem(position);
			if (info != null) {
				return info.id;
			}
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			VideoSummaryInfo info = (VideoSummaryInfo) this.getItem(position);
			
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.video_list_item, parent, false);
			}
			
			TextView titleText = (TextView) convertView.findViewById(R.id.textView1);
			titleText.setText(info.title);
			
			TextView descText = (TextView) convertView.findViewById(R.id.textView2);
			descText.setText(info.description);
			
			return convertView;
		}
		
	}

}
