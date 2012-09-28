package org.systemsbiology.util;


public interface ProgressListener {

	int DEFAULT_AMOUNT = 1000;


	void init(int totalExpectedProgress, String message);

	void init(int totalExpectedProgress);

	/**
	 * Report the amount of progress so far.
	 */
	void setProgress(int progress);

	/**
	 * Increments the progress by some amount.
	 */
	void incrementProgress(int amount);

	/**
	 * Signal that the task has finished.
	 */
	void done();
}
