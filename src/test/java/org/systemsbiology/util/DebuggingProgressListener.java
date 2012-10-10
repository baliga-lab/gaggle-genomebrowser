package org.systemsbiology.util;

import org.systemsbiology.genomebrowser.util.ProgressListener;

public class DebuggingProgressListener implements ProgressListener {
    private String tag = "debug";

    public DebuggingProgressListener() { }
    public DebuggingProgressListener(String tag) { this.tag = tag; }
    public void done() { System.out.println(tag + ": progress done");	}
    public void incrementProgress(int amount) {
        System.out.println(tag + ": progress += " + amount);
    }
    public void init(int expected) {
        System.out.println(tag + ": progress expected = " + expected);
    }
    public void init(int expected, String message) { init(expected); }
    public void setMessage(String message) {
        System.out.println(tag + ": progress message=" + message);
    }
    public void setProgress(int progress) {
        System.out.println(tag + ": progress = " + progress);
    }
}
