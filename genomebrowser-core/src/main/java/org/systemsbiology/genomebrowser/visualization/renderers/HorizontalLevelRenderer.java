package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.Color;
import java.awt.Graphics;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

public class HorizontalLevelRenderer extends QuantitativeTrackRenderer {

	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2, y;
        double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);

		// draw axis
		g.setColor(Color.GRAY);
		g.drawLine(0, y0, params.getDeviceWidth(), y0);

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x00FF00));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

        g.setColor(color);

		for (Feature.Quantitative feature: ((Iterable<Feature.Quantitative>)features)) {
        	x1 = (int) ((feature.getStart() - params.getStart()) * params.getScale());
        	x2 = (int) ((feature.getEnd() - params.getStart()) * params.getScale());
	        y = (int) (y0 - feature.getValue() * yScale);
	        g.drawLine(x1,y,x2,y);
        }
	}
}
