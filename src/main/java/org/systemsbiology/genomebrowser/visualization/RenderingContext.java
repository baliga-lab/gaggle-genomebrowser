package org.systemsbiology.genomebrowser.visualization;

import java.awt.Graphics;
import java.awt.Image;

import org.systemsbiology.genomebrowser.model.FeatureFilter;

/**
 * A blob of references needed to render a block of track data.
 */
public class RenderingContext {
	final TrackRenderer renderer;
	final FeatureFilter filter;
	final int frame;
	final TrackRendererScheduler scheduler;
	final Image image;

	public RenderingContext(int frame, TrackRenderer renderer, FeatureFilter filter, TrackRendererScheduler scheduler, Image image) {
		this.frame = frame;
		this.renderer = renderer;
		this.filter = filter;
		this.scheduler = scheduler;
		this.image = image;
	}

	public Graphics getGraphics() {
		return image.getGraphics();
	}
}
