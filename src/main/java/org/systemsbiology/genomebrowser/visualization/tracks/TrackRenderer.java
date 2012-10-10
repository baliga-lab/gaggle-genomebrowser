package org.systemsbiology.genomebrowser.visualization.tracks;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;


import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Hyperlink;

/**
 * A class that draws a track of data onto the viewer panel.
 * @author cbare
 */
public abstract class TrackRenderer {

	protected Track<? extends Feature> track;

	/**
	 * where on the component to draw the track
	 * expressed in percent
	 */
	protected double top;

	/**
	 * height of track expressed in a percentage
	 */
	protected double height;

	protected Color color = new Color(0x80336699, true);
	protected boolean outline = false;

	/**
	 * Parameters relating screen coordinates to genome position.
	 * Must be set as part of initialization.
	 */
	protected ViewParameters params;



	// dependency
	public void setTrack(Track<? extends Feature> track) {
		this.track = track;
	}

	// dependency
	public void setViewParameters(ViewParameters p) {
		this.params = p;
	}

	public Track<? extends Feature> getTrack() {
		return track;
	}

	public void setColor(Color color) {
		if (color != null)
			this.color = color;
	}

	public Color getColor() {
		return this.color;
	}

	public void setTop(double top) {
		this.top = top;
	}

	public double getTop() {
		return top;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getHeight() {
		return height;
	}

	public void configure(Attributes attr) {
		// set renderer attributes
		if (track.getAttributes().containsKey("color")) {
			setColor(track.getAttributes().getColor("color", getColor()));
		}
		setTop(track.getAttributes().getDouble("top", 0.1));
		setHeight(track.getAttributes().getDouble("height", 0.5));
		outline = track.getAttributes().getBoolean("outline", false);
		// overridden in QuantitativeTrackRenderer to set range min and max
	}

	/**
	 * draw the track at the given position on the component. This method
	 * will be called from GenomeViewPanel.paintComponent(...);
	 */
	// TODO pass sequence here? or Key? or FeatureFilter?
	public abstract void draw(Graphics g, Iterable<? extends Feature> features, Strand strand);

	public List<Feature> getContainedFeatures(Sequence s, Rectangle r) {
		return Collections.emptyList();
	}

	/**
	 * Override this to provide links in the right-click menu.
	 * For example, clicking on a PFAM domain might take you to
	 * the relevant page in the PFAM database.
	 */
	public List<Hyperlink> getLinks(int x, int y) {
		return Collections.emptyList();
	}

	public void deselect() {};

	public boolean containsPoint(Point p) {
		int t = (int)(top * params.getDeviceHeight());
		if (p.y < t)
			return false;
		if (p.y > (t + height * params.getDeviceHeight()))
			return false;
		return true;
	}
}
