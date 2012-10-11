package org.systemsbiology.util;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Support code for a class that provides progress events.
 * @author cbare
 */
public class ProgressListenerSupport
implements Iterable<ProgressListener>, ProgressReporter, ProgressListener {
    Set<ProgressListener> listeners = new CopyOnWriteArraySet<ProgressListener>();

    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    public Iterator<ProgressListener> iterator() {
        return listeners.iterator();
    }

    public void fireIncrementProgressEvent(int amount) {
        for (ProgressListener listener : listeners) {
            listener.incrementProgress(amount);
        }
    }

    public void fireProgressEvent(int progress) {
        for (ProgressListener listener : listeners) {
            listener.setProgress(progress);
        }
    }

    public void fireProgressEvent(int progress, int expected) {
        for (ProgressListener listener : listeners) {
            listener.setExpectedProgress(expected);
            listener.setProgress(progress);
        }
    }

    public void fireSetExpectedProgressEvent(int expected) {
        for (ProgressListener listener : listeners) {
            listener.setExpectedProgress(expected);
        }
    }
    public void fireInitEvent(int expected) {
        for (ProgressListener listener : listeners) {
            listener.init(expected);
        }
    }
    public void fireInitEvent(int expected, String message) {
        for (ProgressListener listener : listeners) {
            listener.init(expected, message);
        }
    }

    public void fireMessageEvent(String message) {
        for (ProgressListener listener : listeners) {
            listener.setMessage(message);
        }
    }

    public void fireDoneEvent() {
        for (ProgressListener listener : listeners) {
            listener.done();
        }
    }

    // ---- progress listener methods -----------------------------------------
	
    // We implement these for forwarding progress from one source to listeners
    // of this source. This usually means that a process that reports progress
    // is being used as a part of a larger process. So, we it makes sense to
    // forward increments and messages, but probably not other progress events

    public void done() {}
    public void init(int expected) {}
    public void init(int expected, String message) {}
    public void setExpectedProgress(int expected) {}
    public void setProgress(int progress) {}
    
    public void incrementProgress(int amount) {
        fireIncrementProgressEvent(amount);
    }

    public void setMessage(String message) {
        fireMessageEvent(message);
    }
}
