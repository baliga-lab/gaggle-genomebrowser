package org.systemsbiology.util;

import java.util.HashSet;
import java.util.Set;

public class Progress {
    private Set<ProgressListener> listeners = new HashSet<ProgressListener>();
    private int totalExpectedProgress;
    private int progress;

    public void addProgressListener(ProgressListener listener) {
        // listeners might be added while loading is in progress
        // if UI finishes initializing part way through loading
        // the track data. If so, init the listener as it is added.
        synchronized (listeners) {
            listeners.add(listener);
            // if total expected > 0, then there's a load in progress
            // we want to initialize the new listener to reflect the
            // current amount of progress
            if (totalExpectedProgress > 0) {
                listener.init(totalExpectedProgress);
                listener.setProgress(progress);
            }
        }
    }

    public void removeProgressListener(ProgressListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public Set<ProgressListener> getListeners() {
        synchronized (listeners) {
            return new HashSet<ProgressListener>(listeners);
        }
    }

    public void fireIncrementProgressEvent(int amount) {
        synchronized (listeners) {
            for (ProgressListener listener : listeners) {
                listener.incrementProgress(amount);
            }
        }
    }

    public void fireSetProgressEvent(int progress) {
        synchronized (listeners) {
            for (ProgressListener listener : listeners) {
                listener.setProgress(progress);
            }
        }
    }

    public void fireInitEvent(int totalExpectedProgress) {
        synchronized (listeners) {
            for (ProgressListener listener : listeners) {
                listener.init(totalExpectedProgress);
            }
        }
    }

    public void fireInitEvent(int totalExpectedProgress, String message) {
        synchronized (listeners) {
            for (ProgressListener listener : listeners) {
                listener.init(totalExpectedProgress, message);
            }
        }
    }

    public void fireDoneEvent() {
        synchronized (listeners) {
            for (ProgressListener listener : listeners) {
                listener.done();
                totalExpectedProgress = 0;
            }
        }
    }
}
