package org.systemsbiology.util.swing;

import javax.swing.SwingUtilities;

import org.systemsbiology.genomebrowser.app.ProgressListener;


/**
 * A wrapper for progressListeners that ensures that all methods will be called
 * on the Swing event thread.
 */
public class SwingThreadProgressListenerWrapper implements ProgressListener {
    private ProgressListener listener;

    /**
     * @param listener A ProgressListener which needs to be called on the
     * Swing event thread.
     */
    public SwingThreadProgressListenerWrapper(ProgressListener listener) {
        this.listener = listener;
    }

    public void done() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.done();
                }
            });
    }

    public void incrementProgress(final int amount) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.incrementProgress(amount);
                }
            });
    }

    public void init(final int expected) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.init(expected);
                }
            });
    }
    public void init(final int expected, final String message) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.init(expected, message);
                }
            });
    }

    public void setExpectedProgress(final int expected) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.setExpectedProgress(expected);
                }
            });
    }

    public void setMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.setMessage(message);
                }
            });
    }

    public void setProgress(final int progress) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    listener.setProgress(progress);
                }
            });
    }
}
