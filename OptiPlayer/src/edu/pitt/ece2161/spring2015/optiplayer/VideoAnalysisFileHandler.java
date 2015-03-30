package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import edu.pitt.ece2161.spring2015.optiplayer.VideoAnalysisDataset.DataEntry;

/**
 * Handles reading and writing the data file for video analysis information.
 * 
 * @author Brian Rupert
 *
 */
public class VideoAnalysisFileHandler {
	
	private static final String ENCODING = "UTF-8";

	public void writeFlatFile(File f, VideoAnalysisDataset data) throws IOException {
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f), Charset.forName(ENCODING)));
			
			List<DataEntry> entries = data.getDataList();
			for (DataEntry entry : entries) {
				w.write(entry.getPosition() + "\t" + entry.getLevel() + "\n");
			}
		} finally {
			if (w != null) {
				w.close();
			}
		}
	}
	
	public VideoAnalysisDataset readFlatFile(File f) throws IOException{
		VideoAnalysisDataset data = null;
		BufferedReader r = null;
		try {
			r = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), Charset.forName(ENCODING)));
			
			data = new VideoAnalysisDataset();
			
			String line = r.readLine();
			while (line != null) {
				String[] fields = line.split("[\t]");
				long position = Long.parseLong(fields[0]);
				int level = Integer.parseInt(fields[1]);
				data.update(position, level);
				
				line = r.readLine();
			}
		} finally {
			if (r != null) {
				r.close();
			}
		}
		return data;
	}
}
