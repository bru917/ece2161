package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Represents the computed brightness levels of a video.
 * 
 * @author Brian Rupert
 *
 */
public class VideoAnalysisDataset {
	
	private String videoId;
	
	private TreeMap<Long, DataEntry> data;
	
	/**
	 * Creates a new data set.
	 */
	public VideoAnalysisDataset() {
		this.data = new TreeMap<Long, DataEntry>();
	}
	
	/**
	 * Inserts data for the given video position.
	 * @param position The position in milliseconds where zero is the start of the video.
	 * @param level The level value.
	 */
	public void update(long position, int level) {
		// Find existing entry for the position.
		DataEntry entry = getEntryExact(position);
		if (entry == null) {
			// Create new entry.
			entry = new DataEntry();
		}
		entry.position = position;
		entry.level = level;
		data.put(position, entry);
	}

	/**
	 * Gets an entry for the exact position requested.
	 * This returns null if a value is not set for that exact position.
	 * @param position
	 * @return
	 */
	public DataEntry getEntryExact(long position) {
		return this.data.get(position);
	}
	
	/**
	 * Gets the best-fit entry at or before the specified position.
	 * <p>
	 * <strong><em>Examples:</em></strong><br/>
	 * Assume positions {@code [200, 1200, 2200]} exist in the data set.
	 * <p>
	 * {@code getEntry(0)} returns null<br/>
	 * {@code getEntry(200)} returns entry at 200<br/>
	 * {@code getEntry(300)} returns entry at 200<br/>
	 * {@code getEntry(1100)} returns entry at 200<br/>
	 * {@code getEntry(1200)} returns entry at 1200<br/>
	 * {@code getEntry(1400)} returns entry at 1200<br/>
	 * 
	 * @param position
	 * @return
	 */
	public DataEntry getEntry(long position) {
		Long exact = data.floorKey(position);
		if (exact == null) {
			return null;
		}
		return data.get(exact);
	}
	
	/**
	 * Clears all entries.
	 */
	public void reset() {
		this.data.clear();
	}
	
	/**
	 * Gets how many entries are contained in this data set.
	 * @return The entry count.
	 */
	public int getCount() {
		return this.data.size();
	}
	
	/**
	 * Gets a copy of all the entries of this set of dimming data ordered
	 * chronologically (i.e. by position)
	 * @return A new list of all entries.
	 */
	public List<DataEntry> getDataList() {
		return new ArrayList<DataEntry>(this.data.values());
	}
	
	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	@Override
	public String toString() {
		return "VideoAnalysisDataset[size=" + this.data.size() + "]";
	}
	
	public static class DataEntry implements Comparable<DataEntry> {
		
		private long position;
		private int level;
		
		/**
		 * Gets the frame position in milliseconds.
		 * @return
		 */
		public long getPosition() {
			return this.position;
		}
		
		/**
		 * Gets the brightness level of the frame.
		 * @return
		 */
		public int getLevel() {
			return this.level;
		}
		
		@Override
		public int compareTo(DataEntry another) {
			if (another == null) {
				return 1;
			}
			return Long.valueOf(this.position).compareTo(another.position);
		}
		
		@Override
		public String toString() {
			return "VideoAnalysisDataset.DataEntry[position=" + this.position
					+ ", level=" + this.level + "]";
		}
	}

}
