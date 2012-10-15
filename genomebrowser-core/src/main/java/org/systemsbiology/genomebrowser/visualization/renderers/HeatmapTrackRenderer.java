package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.ColorScale;
import org.systemsbiology.genomebrowser.visualization.ColorScaleRegistry;
import org.systemsbiology.genomebrowser.util.Attributes;


public class HeatmapTrackRenderer extends QuantitativeTrackRenderer {
	int thickness = 2;
	private double gamma = 0.8;
	private Color outlineColor = new Color(0x33666666, true);

	// ColorScale is the strategy for converting values into colors
	private ColorScale colorScale;
	private ColorScaleRegistry colorScaleRegistry;


	public void configure(Attributes attr) {
		super.configure(attr);
		gamma = track.getAttributes().getDouble("gamma", gamma);
		colorScale = colorScaleRegistry.get(track.getAttributes().getString("color.scale", "red.green"));
		colorScale.setGamma(gamma);
		colorScale.setRange(range);
	}

	public void setColorScaleRegistry(ColorScaleRegistry colorScaleRegistry) {
		this.colorScaleRegistry = colorScaleRegistry;
	}

	public void setThickness(int thickness) {
		this.thickness = thickness;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2;

		int y = (int)(top * params.getDeviceHeight());
		int h = (int)(height * params.getDeviceHeight());

		Iterator<Feature.Quantitative> iterator = ((Iterable<Feature.Quantitative>)features).iterator();
		Feature.Quantitative feature = null;

		while (iterator.hasNext()) {
			feature=iterator.next();
			x1 = params.toScreenX(feature.getStart());
			x2 = params.toScreenX(feature.getEnd());
			int w = Math.max(1, x2 - x1);
			
			g.setColor(colorScale.valueToColor(feature.getValue()));
			g.fillRect(x1, y, w, h);
			
			g.setColor(outlineColor);
			g.drawRect(x1, y, w, h);
		}
	}

	public void setGamma(double gamma) {
		this.gamma  = gamma;
		if (colorScale != null)
			colorScale.setGamma(gamma);
	}

	public void setRange(double min, double max) {
		super.setRange(min, max);
		if (colorScale != null)
			colorScale.setRange(range);
	}
}
