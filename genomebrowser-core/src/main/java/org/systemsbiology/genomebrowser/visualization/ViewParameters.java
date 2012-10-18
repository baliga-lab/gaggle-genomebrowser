package org.systemsbiology.genomebrowser.visualization;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;

// TODO changing chromosomes when you're zoomed way out doesn't rescale properly
// TODO convert from device to genome coordinates in one place

// This class should serve as model for position and zoom, updating listeners
// upon changes. It is unsynchronized and should be owned by the ui package and
// accessed only from the swing event thread.

/**
 * Relates screen coordinates with genome position.
 * Holds the parameters that determine how much of the chromosome and
 * it's associated data tracks are rendered onto the GenomeViewPanel.
 *
 * The viewParameters object is unsynchronized so care must be taken to ensure
 * that it is accessed (either read or write) only on the swing UI thread.
 * The object is "owned" by the UI package, and generally shouldn't be touched
 * outside that package. The exception to this is the renderers, whose paint
 * methods are called on the ui thread.
 */
public class ViewParameters {
	
	private Sequence sequence;
	private double scale;
	private int start;
	private int end;
	private int deviceHeight;
	private int deviceWidth;	
	private int defaultViewSize = 20000;

	public ViewParameters() { }

	/**
	 * scaling factor in pixels per base-pair
	 */
	public double getScale() { return scale; }

	/**
	 * start of displayed range of chromosome
	 */
	public int getStart() { return start; }

	/**
	 * end of displayed range of chromosome
	 * (inclusive)
	 */
	public int getEnd() { return end; }

	public int getWidth() { return end - start + 1; }

	/**
	 * height of the component in pixels
	 */
	public int getDeviceHeight() { return deviceHeight; }

	/**
	 * width of the component in pixels
	 */
	public int getDeviceWidth() { return deviceWidth; }

	public int toGenomeCoordinate(int screenX) {
		return ((int)(screenX / scale)) + start;
	}

	public int toScreenX(int position) {
		return (int)Math.round((position - start) * getScale());
	}

	/**
	 * set the size of the viewing area in pixels
	 */
	public void setDeviceSize(int width, int height) {
		if (width != this.deviceWidth || height != this.deviceHeight) {
			this.deviceWidth = width;
			this.deviceHeight = height;

			// adjust the scale if the window is bigger than the whole chromosome
			if (width / scale > getSequenceLength()) {
				scale = ((double)width) / ((double)getSequenceLength());
			}
			// the end can't be past the end of the chromosome.
			end = start + (int)(width / scale);
			if (end > getSequenceLength()) {
				start = Math.max(0, start - end + getSequenceLength());
				end = getSequenceLength();
			}	
			fireViewParametersChangeEvent();
		}
	}

	/**
	 * Centers the view on the given position without
	 * altering the scale. change the start and end but not the scale.
	 */
	public void centerOnPosition(int position) {
		// get current width
		int w = end - start;
		int start = position - (w >>> 1);
		if (start < 0) start = 0;
		int end = start + w;
		if (end > getSequenceLength()) {
			start -= (end - getSequenceLength());
			end = getSequenceLength();
		}
		setStartAndEnd(start, end);
	}

	public void setStartAndEnd(int start, int end) {
		if (this.start != start || this.end != end) {
			this.start = start;
			this.end = end;
			fireViewParametersChangeEvent();
		}
	}

	public void moveRight(int x) {
		int w = end - start;
		end += x;
		if (end > getSequenceLength()) end = getSequenceLength();
		start = end - w;
		fireViewParametersChangeEvent();
	}

	public void moveLeft(int x) {
		int w = end - start;
		start -= x;
		if (start < 0) start = 0;
		end = start + w;
		fireViewParametersChangeEvent();
	}

	/**
	 * set the visible range of the chromosome in base pairs
	 */
	public void setRange(int start, int end) {
		if (this.start != start || this.end != end) {
			this.start = start;
			this.end = end;
			this.scale = ((double)deviceWidth)/(end - start + 1.0);
			fireViewParametersChangeEvent();
		}
	}

	public void setStart(int start) {
		if (this.start != start) {
			this.end = this.end - this.start + start;
			this.start = start;
			fireViewParametersChangeEvent();
		}
	}

	public int getSequenceLength() {
		return sequence == null ? 0 : sequence.getLength();
	}

	public void setSequence(Sequence sequence) {
		if (this.sequence != sequence) {
			this.sequence = sequence;
			if (end - start + 1 > sequence.getLength()) {
				start = 0;
				end = sequence.getLength();
			}
			fireViewParametersChangeEvent();
		}
	}

	public void initViewParams(Sequence sequence, int start, int end) {
		if (this.sequence != sequence || this.start != start || this.end != end) {
			this.sequence = sequence;
			this.start = start;
			this.end = end;
			this.scale = ((double)deviceWidth)/(end - start + 1.0);
			fireViewParametersChangeEvent();
		}
	}

	public Sequence getSequence() { return sequence; }

	public void initViewParams(Sequence sequence) { //changed dmartinez+rvencio 2012-01-03
		int len = sequence == null ? 0 : sequence.getLength();
		if( start == 0 && end == 0 ){ //if no previous view to preserve, use default
			initViewParams(sequence, 0, Math.min(len, defaultViewSize));
		} else {
			initViewParams(sequence, start, end); //preserve previous view parameters
		}
	}

	public Segment getVisibleSegment() {
		return new Segment(sequence.getSeqId(), start, end);
	}

	/**
	 * 
	 * @param zoom between 0.0 and 1.0 inclusive
	 */
	public void setZoom(double zoom) {
		if (zoom > 1.0) zoom = 1.0;

		// max zoom in to 12 pixels per base pair
		int bps = Math.max(deviceWidth/12, (int)Math.round(getSequenceLength() * zoom));
		setWidthInBasePairs(bps);
	}

	public void zoomIn() {
		double linearZoom1000 = inverseTweak(((double)getWidth()) / getSequenceLength()) - 20.0;
		setZoom(Math.max(tweak(linearZoom1000), 0.0));
	}

	public void zoomOut() {
		double linearZoom1000 = inverseTweak(((double)getWidth()) / getSequenceLength()) + 20.0;
		setZoom(Math.min(tweak(linearZoom1000), 1.0));
	}

	/**
	 * Set width of visible region in base pairs subject to the constraint that
	 * the visible region not exceed the length of the sequence. 
	 * @param bps width of visible region in base pairs. Must be > 0.
	 * @return the min of bps and sequence.getLength()
	 */
	public int setWidthInBasePairs(int bps) {
		if (bps > getSequenceLength()) bps = getSequenceLength();

		// zoom in or out around the midpoint of the visible region
		double mid = (start + end) / 2.0;

		// adjust start and end, making sure neither start nor end fall
		// off the ends of the sequence.
		int start = Math.max(0, (int)(mid - (bps / 2.0)));
		int end = start + bps;

		if (end > getSequenceLength()) {
			start = getSequenceLength() - bps;
			end = getSequenceLength();
		}
		this.scale = (double)deviceWidth / (double)bps;
		setStartAndEnd(start, end);
		
		// listeners notified by setStartAndEnd()
		return bps;
	}

	/**
	 * sets a new scale subject to the constraint that the visible region
	 * not exceed the length of the sequence.
	 * @param proposedScale scale in pixels per base pair
	 * @return the resulting new scale
	 */
	public double setScale(double proposedScale) {
		setWidthInBasePairs((int)(deviceWidth / proposedScale));
		return scale;
	}

	@Override
	public String toString() {
		return String.format("(ViewParameters %d, %d, %f)", start, end, scale);
	}


	// handle listeners
	public interface ViewParametersListener {
		public void viewParametersChanged(ViewParameters p);
	}

	private Set<ViewParametersListener> viewParametersListeners = new CopyOnWriteArraySet<ViewParametersListener>();

	public void addViewParametersListener(ViewParametersListener listener) {
		viewParametersListeners.add(listener);
	}

	public void removeViewParametersListener(ViewParametersListener listener) {
		viewParametersListeners.remove(listener);
	}

	public void fireViewParametersChangeEvent() {
		for (ViewParametersListener listener : viewParametersListeners) {
			listener.viewParametersChanged(this);
		}
	}

	/**
	 * @param x 0 <= x <= 1000
	 * @return a logarithmically scaled number between 0 and 1 (inclusive)
	 */
	public double tweak(double x) {
		return Math.pow(1000, (x / 1000.0)) / 1000.0;
	}

	public int inverseTweak(double y) {
		return (int) (Math.log( y * 1000) / Math.log(1000) * 1000.0);
	}	
}
