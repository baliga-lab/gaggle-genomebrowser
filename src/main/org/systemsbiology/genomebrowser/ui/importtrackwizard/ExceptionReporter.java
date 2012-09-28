package org.systemsbiology.genomebrowser.ui.importtrackwizard;

public interface ExceptionReporter {
	public void reportException(String message, Exception e);
	public void updateStatus();
}
