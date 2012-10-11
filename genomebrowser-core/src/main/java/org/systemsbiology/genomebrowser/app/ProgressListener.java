package org.systemsbiology.genomebrowser.app;

// TODO Unify Progress Listeners

public interface ProgressListener extends org.systemsbiology.util.ProgressListener {
    void setExpectedProgress(int expected);
    void setMessage(String message);
}

