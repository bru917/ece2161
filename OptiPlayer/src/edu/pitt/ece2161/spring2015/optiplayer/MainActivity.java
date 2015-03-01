package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import android.widget.ImageView;
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
	
	private VideoResultsListAdapter listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		listAdapter = new VideoResultsListAdapter();
		
		ListView listView = (ListView) findViewById(R.id.searchList);
		listView.setAdapter(listAdapter);
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
	
	public void clickSearchButton(View view) {
		TextView txt = (TextView) findViewById(R.id.searchText);
		
		List<VideoProperties> newlist = search(txt.getText().toString());
		
		listAdapter.update(newlist);
	}
	
	/**
	 * Executes the search task.
	 * @param queryTerm
	 * @return
	 */
	private List<VideoProperties> search(String queryTerm) {
		VideoSearchTask task = new VideoSearchTask(this);
		
		task.execute(queryTerm);
		
		List<VideoProperties> results = null;
		try {
			results = task.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return results;
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
	 * This adapter controls the display of the video list.
	 * 
	 * @author Brian Rupert
	 *
	 */
	private class VideoResultsListAdapter extends BaseAdapter implements ListAdapter {
		
		private List<VideoProperties> delegate = new ArrayList<VideoProperties>();
		
		VideoResultsListAdapter() {
			
		}
		
		public void update(List<VideoProperties> newlist) {
			this.delegate.clear();
			if (newlist != null) {
				this.delegate.addAll(newlist);
			}
			notifyDataSetChanged();
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
			
			final ImageView img = (ImageView) convertView.findViewById(R.id.resultImage);
			if (info.getThumbnail() != null) {
				img.setImageBitmap(info.getThumbnail());
			}
			
			return convertView;
		}
		
	}

}
