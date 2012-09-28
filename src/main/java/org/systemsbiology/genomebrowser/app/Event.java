package org.systemsbiology.genomebrowser.app;


// TODO: write a script to find all event action strings

/**
 * An event with a source and an arbitrary action. Optionally, events can
 * specify that they require the UI to repaint itself.
 * 
 * @author cbare
 */
public class Event {
	private final Object source;
	private final Object data;
	private final long timestamp;
	private final String action;
	private final boolean repaint;


	public Event(Object source, String action) {
		this.source = source;
		this.action = action;
		this.repaint = false;
		this.timestamp = System.currentTimeMillis();
		this.data = null;
	}

	public Event(Object source, String action, boolean repaint) {
		this.source = source;
		this.action = action;
		this.repaint = repaint;
		this.timestamp = System.currentTimeMillis();
		this.data = null;
	}

	public Event(Object source, String action, Object data) {
		this.source = source;
		this.action = action;
		this.repaint = false;
		this.timestamp = System.currentTimeMillis();
		this.data = data;
	}

	public Event(Object source, String action, Object data, boolean repaint) {
		this.source = source;
		this.action = action;
		this.repaint = repaint;
		this.timestamp = System.currentTimeMillis();
		this.data = data;
	}


	public Object getSource() {
		return source;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getAction() {
		return action;
	}

	public boolean requiresRepaint() {
		return repaint;
	}

	public Object getData() {
		return data;
	}

	public String toString() {
		return String.format("(Event src=%s, action=%s, object=%s)", String.valueOf(source), action, String.valueOf(data));
	}
}
