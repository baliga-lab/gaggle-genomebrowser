package org.systemsbiology.util;

public interface ProgressReporter {
    void addProgressListener(ProgressListener listener);
    void removeProgressListener(ProgressListener listener);
}
