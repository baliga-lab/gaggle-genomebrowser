package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.Color;
import java.awt.Graphics;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * A track that plots data with a single scalar value
 * per point as a little circle (or bubble) like R does.
 */
public class BubbleTrackRenderer extends QuantitativeTrackRenderer {
	private static final Logger log = Logger.getLogger(BubbleTrackRenderer.class);

	public BubbleTrackRenderer() {
		color = new Color(0x33,0x66,0x99);
	}
	
	public void configure(Attributes attr) {
		super.configure(attr);
		outline = attr.getBoolean("outline", false);
	}

//	public void draw(Graphics g) {
//		int x, y;
//		double yScale = params.height * height / (range.max - range.min);
//		int y0 = (int) (params.height * top + params.height * height + range.min * yScale);
//
//		if (outline) {
//			int _t = (int) (top * params.height);
//			int _h = (int) (height * params.height);
//			g.setColor(new Color(0x3300FF));
//			g.drawRect(0, _t+1, params.width-1, _h-1);
//		}
//
//		g.setColor(color);
//
//		for (Feature.Quantitative feature: track.features(params.start, params.end)) {
//			x = params.toScreenX(feature.getCentralPosition());
//			y = (int) (y0 - feature.getValue() * yScale);
//			g.drawOval(x-2, y-2, 4, 4);
//		}
//	}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y;
		double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x3300FF));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

		g.setColor(color);

		int n = 0;
		@SuppressWarnings("unchecked")
		Iterable<Feature.Quantitative> quantitativeFeatures = (Iterable<Feature.Quantitative>)features;
		for (Feature.Quantitative feature: quantitativeFeatures) {
			x = params.toScreenX(feature.getCentralPosition());
			y = (int) (y0 - feature.getValue() * yScale);
			g.drawOval(x-2, y-2, 4, 4);
			n++;
		}
		log.info("rendered " + n + " features.");
	}
}
