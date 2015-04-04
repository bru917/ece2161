package edu.pitt.ece2161.spring2015.optiplayer;

/**
 * This interface defines the methods that are common to both the analysis
 * and playback background tasks.
 * 
 * @author Brian Rupert
 */
public interface VideoBackgroundTask {
	
	/**
	 * Gets the task execution cycle time in milliseconds.
	 * @return Execution cycle time (ms)
	 */
	int getCycleTime();

	/**
	 * Sets the state of the task to running (i.e the video is playing)
	 */
	void setRunning();

	/**
	 * Sets the state of the task to paused (i.e. the video is not playing)
	 */
	void setPaused();

	/**
	 * Cancels the task. The task should prepare to be released.
	 * @return A response to the cancellation request (usually true)
	 */
	boolean cancel();

}
