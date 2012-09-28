package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Color;
import java.awt.Graphics;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;



/**
 * A Renderer for highlighting regions of the genome.
 */
public class HighlightTrackRenderer extends TrackRenderer {

	public HighlightTrackRenderer() {
		color = new Color(0x20FF0000);
	}

//	public void configure(Attributes attr) {
//		super.configure(attr);
//	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2;
		int y0 = (int) (params.getDeviceHeight() * top);
		int h = (int) (params.getDeviceHeight() * height);

		g.setColor(color);

		for (Feature feature: (Iterable<Feature>)features) {
			x1 = params.toScreenX(feature.getStart());
			x2 = params.toScreenX(feature.getEnd());
			g.fillRect(x1, y0, x2-x1+1, h);
		}
	}
}
