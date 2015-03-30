package edu.pitt.ece2161.spring2015.optiplayer;

public interface VideoBackgroundTask {
	
	int getCycleTime();

	void setRunning();

	void setPaused();

	boolean cancel();

}
