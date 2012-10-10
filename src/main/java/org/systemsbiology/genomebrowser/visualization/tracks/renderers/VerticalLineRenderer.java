package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.Attributes;


/**
 * A track renderer that plots data with a position and a single scalar value
 * as a vertical line
 */
public class VerticalLineRenderer extends QuantitativeTrackRenderer {
	float weight = 1.0f;
	double filter = 0.0;

	@SuppressWarnings("unchecked")
	@Override
	public void configure(Attributes attr) {
		this.color = Color.RED;
		super.configure(attr);
		if (attr.containsKey("weight")) {
			this.weight = attr.getFloat("weight", 1.0f);
		}
		// TODO filtering features based on values or attributes
		// a temporary hack until a more complete framework for filtering is designed.
		if (attr.containsKey("filter.percent")) {
			Range trueRange = ((Track.Quantitative<Feature.Quantitative>)track).getRange();
			filter = trueRange.percentileOfMax(attr.getDouble("filter.percent"));
			System.out.println("filter.percent= " + attr.getDouble("filter.percent"));
			System.out.println("range= " + trueRange);
		}
		else {
			filter = Double.MIN_VALUE;
		}
		System.out.println("filter = " + filter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y;
		double top = (strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;
        double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height);
//		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);

        if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x0066FF));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
        }

        g.setColor(color);
        Graphics2D g2d = (Graphics2D)g;
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(weight));

		for (Feature.Quantitative feature: ((Iterable<Feature.Quantitative>)features)) {
			if (feature.getValue()>filter) {
	        	x = params.toScreenX(feature.getCentralPosition());
		        y = (int) Math.round(y0 - feature.getValue() * yScale);
		        g.drawLine(x,y0,x,y);
			}
        }

        g2d.setStroke(oldStroke);
	}
}
