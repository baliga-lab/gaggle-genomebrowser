package org.systemsbiology.genomebrowser.app;

import org.systemsbiology.util.Pair;
import org.systemsbiology.util.Progress;

public class RunnableProgressReporter implements ProgressReporter, Runnable {
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();
	private Progress progress;
	private volatile boolean done;

	public RunnableProgressReporter(Progress progress) {
		this.progress = progress;
	}

	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
		// this might not be strictly correct. The listener might get
		// two done notifications, but I don't think it could get none?
		if (done) listener.done();
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}

	public void done() {
		done = true;
	}

	public void run() {
		while (!done) {
			Pair<Integer, Integer> pair = progress.getProgressAndExpected();
			progressListenerSupport.fireSetExpectedProgressEvent(pair.getSecond());
			progressListenerSupport.fireProgressEvent(pair.getFirst());
			try {
				Thread.sleep(400);
			}
			catch (InterruptedException e) {
				done = true;
			}

			if (Thread.currentThread().isInterrupted())
				done = true;
		}
		Pair<Integer, Integer> pair = progress.getProgressAndExpected();
		progressListenerSupport.fireSetExpectedProgressEvent(pair.getSecond());
		progressListenerSupport.fireProgressEvent(pair.getFirst());
		progressListenerSupport.fireDoneEvent();
	}

	public void start() {
		new Thread(this).start();
	}
}
