package edu.pitt.ece2161.spring2015.optiplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the computed brightness levels of a video.
 * 
 * @author Brian Rupert
 *
 */
public class VideoAnalysisDataset {
	
	private Map<Long, DataEntry> data;
	
	/**
	 * Creates a new data set.
	 */
	public VideoAnalysisDataset() {
		this.data = new TreeMap<Long, DataEntry>();
	}
	
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

	public DataEntry getEntryExact(long position) {
		return this.data.get(position);
	}
	
	/**
	 * Gets a copy of all the entries of this set of dimming data ordered
	 * chronologically (i.e. by position)
	 * @return
	 */
	public List<DataEntry> getDataList() {
		return new ArrayList<DataEntry>(this.data.values());
	}
	
	/**
	 * Gets the dimming level entry at or after the specified position.
	 * @param position
	 * @return
	 */
	public DataEntry getEntry(long position) {
		return null;
	}
	
	@Override
	public String toString() {
		return "DimmingData[size=" + this.data.size() + "]";
	}
	
	public static class DataEntry implements Comparable<DataEntry> {
		
		private long position;
		private int level;
		
		public long getPosition() {
			return this.position;
		}
		
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
	}
}
