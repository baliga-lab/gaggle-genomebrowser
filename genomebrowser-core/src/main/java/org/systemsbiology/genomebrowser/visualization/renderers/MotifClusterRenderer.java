package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * A track renderer that plots data with a position and a single scalar value as a
 * blocky line graph, assuming all unspecified data points have 0 value.
 */
public class MotifClusterRenderer extends QuantitativeTrackRenderer {
	float weight = 1.0f;
	double filter = 0.0;

	// the renderer will look for a data point every step base pairs.
	// missing data points will be assumed to be 0.
	int step = 1;

	@SuppressWarnings("unchecked")
	@Override
	public void configure(Attributes attr) {
		this.color = Color.RED;
		super.configure(attr);
		this.weight = attr.getFloat("weight", 1.0f);

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
		int x, y, y_previous, x_previous;
		int position, position_previous;
		double top = (strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;
        double yScale = params.getDeviceHeight() * height / range.max;
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height);

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
        
        position = params.toGenomeCoordinate(0);
        x = 0;
        y = y0;

		Iterator<Feature.Quantitative> iterator = (Iterator<Feature.Quantitative>)features.iterator();
		Feature.Quantitative feature = null;

		while (iterator.hasNext()) {
			feature=iterator.next();

//				if (feature.getValue()>filter) {
//				}

			position_previous = position;
			y_previous = y;
			x_previous = x;
			position = feature.getCentralPosition();
			x = params.toScreenX(position);
			y = (int) (y0 - feature.getValue() * yScale);

			if (position - position_previous <= step) {
		        g.drawLine(x_previous,y_previous,x,y);
			}
			else {
		        g.drawLine(x_previous,y_previous,x_previous,y0);
		        g.drawLine(x_previous,y0,x,y0);
		        g.drawLine(x,y0,x,y);
			}
		}
		
		if (x < params.getDeviceWidth()) {
	        g.drawLine(x,y,x,y0);
	        g.drawLine(x,y0,params.getDeviceWidth(),y0);
		}

        g2d.setStroke(oldStroke);
	}
}
