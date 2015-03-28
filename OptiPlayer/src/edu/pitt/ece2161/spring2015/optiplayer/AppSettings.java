package edu.pitt.ece2161.spring2015.optiplayer;

import android.os.Environment;

/**
 * This class manages our app settings.
 * 
 * @author Brian Rupert
 *
 */
public final class AppSettings {
	
	/**
	 * When enabled, debug messages are output.
	 */
	public static final boolean DEBUG = true;
	
	private String storageFolder;
	
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
}
