package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.visualization.TrackRenderer;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * A base class for renderers of quantitative data tracks.
 * @author cbare
 */
public abstract class QuantitativeTrackRenderer extends TrackRenderer {
	/**
	 * range specified in attributes or if none, the true range.
	 * The true range can always be found on the track itself.
	 */
	protected Range range = new Range(0.0, 1.0);

	@SuppressWarnings("unchecked")
	public void setTrack(Track<? extends Feature> track) {
		this.track = track;
		range = new Range(((Track.Quantitative<? extends Feature.Quantitative>)track).getRange());
	}

	public void setRange(double min, double max) {
		range = new Range(min, max);
	}

	public void configure(Attributes attr) {
		super.configure(attr);
		if (attr.containsKey("rangeMin") && attr.containsKey("rangeMax"))
			setRange(attr.getDouble("rangeMin"), attr.getDouble("rangeMax"));
	}
}
