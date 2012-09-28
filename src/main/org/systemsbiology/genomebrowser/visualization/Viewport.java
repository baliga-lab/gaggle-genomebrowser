package org.systemsbiology.genomebrowser.visualization;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.systemsbiology.genomebrowser.model.Sequence;


// this is what ViewParameters should morph into...

/**
 * Holds the state of the window through which we view the genome. Responsible
 * for transforming genome coordinates to device coordinates.
 * 
 * @author cbare
 */
public class Viewport {
	private Sequence sequence;
	private int start;
	private int end;
	private double scale;

	private int deviceWidth;
	private int deviceHeight;

	
	public Sequence getSequence() {
		return sequence;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public double getScale() {
		return scale;
	}

	public int getDeviceWidth() {
		return deviceWidth;
	}

	public int getDeviceHeight() {
		return deviceHeight;
	}


	/**
	 * Move the viewport left or right without changing scale.
	 */
	public void pan(int start, int end) {
		
	}

	public void setViewport(Sequence sequence, int start, int end) {
		if (this.sequence != sequence || this.start != start || this.end != end) {
			this.sequence = sequence;
			this.start = start;
			this.end = end;
			this.scale = ((double)deviceWidth)/(end - start + 1.0);
			fireViewportChangedEvent();
		}
	}

	public int toGenomeCoordinate(int screenX) {
		return ((int)(screenX / scale)) + start;
	}

	public int toScreenX(int position) {
		return (int)Math.round((position - start) * scale);
	}

	
	// ---- listeners -------------------------------------------------

	public interface ViewportListener {
		public void viewportChanged(Viewport viewport);
	}

	Set<ViewportListener> listeners = new CopyOnWriteArraySet<ViewportListener>();

	public void addViewportListener(ViewportListener listener) {
		listeners.add(listener);
	}

	public void removeViewportListener(ViewportListener listener) {
		listeners.remove(listener);
	}

	public void fireViewportChangedEvent() {
		for (ViewportListener listener : listeners) {
			listener.viewportChanged(this);
		}
	}
}
