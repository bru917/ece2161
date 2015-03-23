package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import android.os.Environment;
import android.util.Log;

public class DimmingFileHandler {
	
	private static final String TAG = "DimmingFileHandler";
	
	private File file;
	
	public DimmingFileHandler() {
		
	}
	
	
	// ****************************************************Write to Dimmming
	// File**************************************************
	void writefile(String text) {
		try {
			OutputStreamWriter myOutWriter = new OutputStreamWriter(new FileOutputStream(file, true));
			myOutWriter.append(text);
			myOutWriter.close();
		} catch (Exception e) {
		}
	}
	
	public void append(int level, float pos) {
		writefile("\n TEST                 " + level + "                 " + pos + "(sec)");
	}

	// ******************************************************Read Dimming
	// File************************************************
	
	String[][] r_dimfile = new String[10000][2];

	void readfile(File file) {
		int num_lines = 0;
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				Log.d(TAG, line);
				num_lines++;
				if (num_lines > 3) {

					StringTokenizer st = new StringTokenizer(line, "TEST ");
					while (st.hasMoreTokens()) {
						String Llevel = st.nextToken();
						String time = st.nextToken();
						r_dimfile[num_lines][0] = Llevel;
						r_dimfile[num_lines][1] = time;
						// Log.d(TAG,"num_lines  "+Llevel + "\t" + time);
					}

				}
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "num_lines" + Integer.toString(num_lines));

		// String s1="TEST---12---120.324";
		// Log.d(TAG,"num_lines  "+s1);

		/**/

	}

	// *************************************************Check for Dimming
	// File*****************************************************
	public File openfile() {
		File FileDir = Environment.getExternalStorageDirectory();
		File dir = new File(FileDir.getAbsolutePath() + "/download");
		dir.mkdirs();
		File file = new File(dir, "DimmingFile1.txt");
		file.setWritable(true);
		if (file.exists()) {
			file.canRead();
			file.setWritable(true);

		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}

}
