package org.systemsbiology.genomebrowser.app;

public interface ProgressReporter {
	public void addProgressListener(ProgressListener listener);
	public void removeProgressListener(ProgressListener listener);
}
