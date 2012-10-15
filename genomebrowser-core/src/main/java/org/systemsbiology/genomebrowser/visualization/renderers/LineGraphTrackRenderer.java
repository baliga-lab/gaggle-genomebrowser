package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

public class LineGraphTrackRenderer extends QuantitativeTrackRenderer {
	float weight = 1.0f;

	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		this.weight = attr.getFloat("weight", 1.0f);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x, y;
        int x1, y1;
        double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min*yScale);

        g.setColor(color);

        Graphics2D g2d = (Graphics2D)g;
        Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(new BasicStroke(weight));

        // TODO expand window when needed?
        // Bug: we really need the feature to the right of the screen and the feature
        // to the left of the screen in order to draw the line properly. We don't necessarily
        // get that. If the track has start and end coordinates, we may only get one or no
        // data points which isn't enough to make a line.

		Iterator<Feature.Quantitative> iterator = ((Iterable<Feature.Quantitative>)features).iterator();
		Feature.Quantitative feature = null;
		if (iterator.hasNext()) {

			feature = iterator.next();
	        x1 = params.toScreenX(feature.getCentralPosition());
	        y1 = (int) (y0 - feature.getValue() * yScale);

	        while (iterator.hasNext()) {
	        	feature=iterator.next();
		        x = params.toScreenX(feature.getCentralPosition());
		        y = (int) (y0 - feature.getValue() * yScale);
		        g.drawLine(x1, y1, x, y);
		        x1 = x;
		        y1 = y;
	        }
        }

		g2d.setStroke(oldStroke);
	}
}
