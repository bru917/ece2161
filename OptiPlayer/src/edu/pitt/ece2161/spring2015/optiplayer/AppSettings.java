package edu.pitt.ece2161.spring2015.optiplayer;

import java.io.File;

import android.os.Environment;

/**
 * This class manages our app settings.
 * 
 * @author Brian Rupert
 *
 */
public final class AppSettings {
	
	private String storageFolder;
	
	private boolean debugMode;
	
	private static AppSettings instance;
	
	public static final AppSettings getInstance() {
		if (instance == null) {
			instance = new AppSettings();
		}
		return instance;
	}
	
	private AppSettings() {
		storageFolder = Environment.getExternalStorageDirectory() + "/OptiPlayer/";
	}

	public String getStorageFolder() {
		return storageFolder;
	}

	public File getLocalAnalysisFile(String videoId) {
		File f = new File(this.storageFolder + "/opt_" + videoId + ".txt");
		return f;
	}
	
	void setDebugMode(boolean b) {
		this.debugMode = b;
	}
	
	public boolean isDebugMode() {
		return debugMode;
	}
	
	public boolean isDeveloperMode() {
		return true;
	}
}
