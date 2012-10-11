package org.systemsbiology.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

// TODO should be unified with application events?

/**
 * holds references to action listeners
 */
public class ActionListenerSupport {
    private Set<ActionListener> listeners =
        new CopyOnWriteArraySet<ActionListener>();

    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    public void fireActionEvent(ActionEvent event) {
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }
}
