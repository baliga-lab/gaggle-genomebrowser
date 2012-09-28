package org.systemsbiology.util;

import org.systemsbiology.genomebrowser.app.ProgressListener;


public class DebuggingProgressListener implements ProgressListener {
	String tag = "debug";

	public DebuggingProgressListener() {
	}

	public DebuggingProgressListener(String tag) {
		this.tag = tag;
	}


	public void done() {
		System.out.println(tag + ": progress done");
	}

	public void incrementProgress(int amount) {
		System.out.println(tag + ": progress += " + amount);
	}

	public void setExpectedProgress(int expected) {
		System.out.println(tag + ": progress expected = " + expected);
	}

	public void setMessage(String message) {
		System.out.println(tag + ": progress message=" + message);
	}

	public void setProgress(int progress) {
		System.out.println(tag + ": progress = " + progress);
	}
}
