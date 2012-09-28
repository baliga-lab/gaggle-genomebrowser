package org.systemsbiology.genomebrowser.app;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


public class EventSupport {
	private Set<EventListener> listeners = new CopyOnWriteArraySet<EventListener>();

	public void addEventListener(EventListener listener) {
		listeners.add(listener);
	}

	public void removeEventListener(EventListener listener) {
		listeners.remove(listener);
	}

	public void fireEvent(Event event) {
		for (EventListener listener : listeners) {
			listener.receiveEvent(event);
		}
	}

	public void fireEvent(Object source, String action) {
		fireEvent(new Event(source, action));
	}

	public void fireEvent(Object source, String action, boolean repaint) {
		fireEvent(new Event(source, action, repaint));
	}

	public void fireEvent(Object source, String action, Object data) {
		fireEvent(new Event(source, action, data));
	}

	public void fireEvent(Object source, String action, Object data, boolean repaint) {
		fireEvent(new Event(source, action, data, repaint));
	}
}
