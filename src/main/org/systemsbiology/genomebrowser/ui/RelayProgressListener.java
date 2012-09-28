package org.systemsbiology.genomebrowser.ui;

import java.util.HashSet;
import java.util.Set;

import org.systemsbiology.util.ProgressListener;


/**
 * Relays progress events to a set of progress listeners.
 *
 * This class is used to track progress of loading track data, which
 * may start before the GUI is rendered. The progress listener that
 * draws the pop-up progress bar is dependent on the GUI being created
 * so we use this class to receive progress events. When the GUI is
 * ready, we add a listener to the RelayProgressListener.
 * RelayProgressListener then initializes the GUI dependent listener
 * to show progress that has occurred so far.
 *
 * @author cbare
 */
public class RelayProgressListener implements ProgressListener {
	int progress;
	int expected;
	String message;
	Set<ProgressListener> listeners = new HashSet<ProgressListener>(); 


	public synchronized void done() {
		progress = 0;
		expected = 0;
		message = null;
		for (ProgressListener listener : listeners) {
			listener.done();
		}
	}

	public synchronized void incrementProgress(int amount) {
		progress += amount;
		for (ProgressListener listener : listeners) {
			listener.incrementProgress(amount);
		}
	}

	public synchronized void init(int totalExpectedProgress, String message) {
		expected = totalExpectedProgress;
		this.message = message;
		for (ProgressListener listener : listeners) {
			listener.init(totalExpectedProgress, message);
		}
	}

	public synchronized void init(int totalExpectedProgress) {
		expected = totalExpectedProgress;
		for (ProgressListener listener : listeners) {
			listener.init(totalExpectedProgress);
		}
	}

	public synchronized void setProgress(int progress) {
		this.progress = progress;
		for (ProgressListener listener : listeners) {
			listener.setProgress(progress);
		}
	}

	/**
	 * The listener may be added while something is already in progress. If
	 * so, we initialize the listener to the currently elapsed amount of progress.
	 */
	public synchronized void addProgressListener(ProgressListener listener) {
		if (expected > 0) {
			if (message == null)
				listener.init(expected);
			else
				listener.init(expected, message);
			listener.setProgress(progress);
		}
		listeners.add(listener);
	}

	public synchronized void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}
}
