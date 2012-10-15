package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

/**
 * Renders segmentation. Segmentation is derived from transcript signal where
 * we expect the signal to be in flat sections like a step function. Each
 * feature in a segmentation track consists of start and end coordinates plus
 * a measurement. The measurement is assumed to be constant over the extent
 * of the segment. 
 */
public class SegmentationTrackRenderer extends QuantitativeTrackRenderer {

	public void configure(Attributes attr) {
		super.configure(attr);
		color = attr.getColor("color", Color.RED);
	}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		double top = (strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;
		int xs, xe, xep, y, yp;
		double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);

		g.setColor(color);

		@SuppressWarnings("unchecked")
		Iterator<Feature.Quantitative> iterator = (Iterator<Feature.Quantitative>)features.iterator();
		Feature.Quantitative feature = null;
		if (iterator.hasNext()) {

			feature = iterator.next();
			xs = (int) ((feature.getStart() - params.getStart()) * params.getScale());
			xe = (int) ((feature.getEnd() - params.getStart()) * params.getScale());
			y = (int) (y0 - feature.getValue() * yScale);
			g.drawLine(xs, y, xe, y);

			while (iterator.hasNext()) {
				feature=iterator.next();
				xep = xe;
				yp = y;
				xs = (int) ((feature.getStart() - params.getStart()) * params.getScale());
				xe = (int) ((feature.getEnd() - params.getStart()) * params.getScale());
				y = (int) (y0 - feature.getValue() * yScale);
				g.drawLine(xep, yp, xs, y);
				g.drawLine(xs, y, xe, y);
			}
		}
	}
}
