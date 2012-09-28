package org.systemsbiology.util;

import org.apache.log4j.Logger;

public class LoggingProgressListener implements  org.systemsbiology.genomebrowser.app.ProgressListener {
	Logger log;

	public LoggingProgressListener(Logger log) {
		this.log = log;
	}

	public LoggingProgressListener(String logger) {
		this.log = Logger.getLogger(logger);
	}

	public void incrementProgress(int amount) {
		log.info("increment progress = " + amount);
	}

	public void setExpectedProgress(int expected) {
		log.info("expected progress = " + expected);
	}

	public void setProgress(int progress) {
		log.info("progress = " + progress);
	}

	public void setMessage(String message) {
		log.info("progress message = " + message);
	}

	public void done() {
		log.info("progress done!");
	}
}
