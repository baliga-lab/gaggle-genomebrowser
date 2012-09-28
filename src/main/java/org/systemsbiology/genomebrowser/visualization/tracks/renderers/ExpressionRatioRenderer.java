package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.ColorScale;
import org.systemsbiology.genomebrowser.visualization.ColorScaleRegistry;
import org.systemsbiology.util.Attributes;


/**
 * Render expression data as line segments colored with heatmap coloring.
 * Red = high, green = low.
 */
public class ExpressionRatioRenderer extends QuantitativeTrackRenderer {
//	private boolean drawZeroLine;
	private int thickness = 2;

	// ColorScale is the strategy for converting values into colors
	private ColorScale colorScale;
	private ColorScaleRegistry colorScaleRegistry;


	public void configure(Attributes attr) {
		super.configure(attr);
		colorScale = colorScaleRegistry.get(track.getAttributes().getString("color.scale", "red.green"));
		colorScale.setGamma(track.getAttributes().getDouble("gamma", 0.6));
		colorScale.setRange(range);
	}

	public void setColorScaleRegistry(ColorScaleRegistry colorScaleRegistry) {
		this.colorScaleRegistry = colorScaleRegistry;
	}

//	public void setDrawZeroLine(boolean drawZeroLine) {
//		this.drawZeroLine = drawZeroLine;
//	}

	public void setThickness(int thickness) {
		this.thickness = thickness;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2;
		int y;
		double yScale = params.getDeviceHeight() * height / (range.max - range.min);
		double top = (strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;
		int y0 = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height + range.min * yScale);
		Stroke s2 = new BasicStroke(thickness);

		// draw axis
//		if (drawZeroLine) {
//			g.setColor(new Color(0xDDDDDD));
//			g.drawLine(0, y0, params.getDeviceWidth(), y0);
//		}

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x00FF00));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

		for (Feature.Quantitative feature: ((Iterable<Feature.Quantitative>)features)) {
			x1 = (int) ((feature.getStart() - params.getStart()) * params.getScale());
			x2 = (int) ((feature.getEnd() - params.getStart()) * params.getScale());
			// make sure width isn't zero.
			if (x2==x1) x2++;
			y = y0 - (int)(feature.getValue() * yScale);

			g.setColor(colorScale.valueToColor(feature.getValue()));
			Stroke s = ((Graphics2D)g).getStroke();
			((Graphics2D)g).setStroke(s2);
			g.drawLine(x1,y,x2,y);
			((Graphics2D)g).setStroke(s);
		}
	}

	public void setGamma(double gamma) {
		if (colorScale != null)
			colorScale.setGamma(gamma);
	}

	public void setRange(double min, double max) {
		super.setRange(min, max);
		if (colorScale != null)
			colorScale.setRange(range);
	}
}
