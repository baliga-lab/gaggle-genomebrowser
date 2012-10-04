package org.systemsbiology.util;

public interface DialogListener {
    public void ok(String action, Object result);
    public void cancel();
    public void error(String message, Exception e);
}
