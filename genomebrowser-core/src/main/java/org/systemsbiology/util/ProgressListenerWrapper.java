package org.systemsbiology.util;

public class ProgressListenerWrapper implements ProgressListener {
    private ProgressListenerSupport progressListenerSupport;
    private int start;
    private int width;
    private int expected = 100;

    public ProgressListenerWrapper() {
        progressListenerSupport = new ProgressListenerSupport();
    }

    public ProgressListenerWrapper(ProgressListenerSupport progressListenerSupport) {
        this.progressListenerSupport = progressListenerSupport;
    }

    /**
     * Scale the progress reporting to fit within a subinterval of
     * the progress of a larger process. Let's say we have a 5 step
     * process. Step 2 reports its own progress on a scale of 0-100.
     * We want to adapt the progress of step 2 to cover the range of
     * 20-20
     * @param start
     * @param end
     */
    public void scaleProgressToFit(int start, int end) {
        this.start = start;
        this.width = end - start;
    }

    public void done() {
        progressListenerSupport.fireDoneEvent();
    }

    public void incrementProgress(int amount) {
        progressListenerSupport.fireIncrementProgressEvent(amount);
    }

    public void init(int expected) {
        this.expected = expected;
        progressListenerSupport.fireInitEvent(expected);
    }
    public void init(int expected, String message) {
        this.expected = expected;
        progressListenerSupport.fireInitEvent(expected, message);
    }

    public void setExpectedProgress(int expected) {
        this.expected = expected;
        if (width <= 0)	progressListenerSupport.fireSetExpectedProgressEvent(expected);
    }

    public void setMessage(String message) {
        progressListenerSupport.fireMessageEvent(message);
    }

    public void setProgress(int progress) {
        int scaledProgress = width > 0 ?
            (int)(start + ((double)progress) / ((double)expected) * width) :  progress;
        progressListenerSupport.fireProgressEvent(scaledProgress);
    }

    public void addProgressListener(ProgressListener listener) {
        progressListenerSupport.addProgressListener(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
        progressListenerSupport.removeProgressListener(listener);
    }
}
