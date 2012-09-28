package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.ColorScale;
import org.systemsbiology.genomebrowser.visualization.ColorScaleRegistry;
import org.systemsbiology.util.Attributes;


public class HeatmapMatrixTrackRenderer extends QuantitativeTrackRenderer {
	int thickness = 2;
	private double gamma = 0.8;
	private boolean overlap;
	private Color outlineColor = new Color(0x33666666, true);
	private boolean splitStrands = false;

	// ColorScale is the strategy for converting values into colors
	private ColorScale colorScale;
	private ColorScaleRegistry colorScaleRegistry;


	public void configure(Attributes attr) {
		super.configure(attr);
		gamma = track.getAttributes().getDouble("gamma", gamma);
		colorScale = colorScaleRegistry.get(track.getAttributes().getString("color.scale", "red.green"));
		colorScale.setGamma(gamma);
		colorScale.setRange(range);
		splitStrands = track.getAttributes().getBoolean("split.strands", false);
		overlap = track.getAttributes().getBoolean("overlap", true);
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

		double top = (splitStrands && strand == Strand.reverse) ? 1.0 - this.top - this.height : this.top;

		int y = (int)(top * params.getDeviceHeight());
		int h = (int)(height * params.getDeviceHeight());

		Iterator<Feature.Matrix> iterator = ((Iterable<Feature.Matrix>)features).iterator();
		Feature.Matrix feature = null, nextFeature = null;
		
		// Problem:
		// Heatmaps, such as those for tiling arrays, may have overlapping probes,
		// so the start of the next probe comes before the start of the next probe.
		// To render these efficiently, we take the end of the probe to be equal to
		// the beginning of the next probe.
		// But, this looks terrible for heatmaps that are resolved to the gene
		// level where there should be gaps between them. I added a secret flag for
		// this case. Setting overlap to false prevents the default behaviour
		// of assuming the probes overlap.

		if (overlap) {
			feature=iterator.hasNext() ? iterator.next() : null;
			while (feature != null) {
				x1 = params.toScreenX(feature.getStart());

				if (iterator.hasNext()) {
					nextFeature = iterator.next();
					x2 = params.toScreenX(nextFeature.getStart());
				}
				else {
					nextFeature = null;
					x2 = params.toScreenX(feature.getEnd());
				}
				
				int w = Math.max(1, x2 - x1);

				double[] values = feature.getValues();

				double yv; // the y coordinate for the value
				double yvn = y; // the y coordinate of the next value
				double increment = ((double)h) / ((double)values.length);

				int yvi; // yv rounded to the nearest integer
				int yvni = (int)Math.round(yvn); // yvn rounded to the nearest integer

				for (int i=0; i<values.length; i++) {
					g.setColor(colorScale.valueToColor(values[i]));
					yv = yvn;
					yvi = yvni;
					yvn = yv + increment;
					yvni = (int)Math.round(yvn);
					g.fillRect(x1, yvi, w, yvni-yvi+1);
				}

				g.setColor(outlineColor);
				g.drawRect(x1, y, w, h);
				feature=nextFeature;
			}
		}
		else {
			while (iterator.hasNext()) {
				feature=iterator.next();
				x1 = params.toScreenX(feature.getStart());
				x2 = params.toScreenX(feature.getEnd());

				int w = Math.max(1, x2 - x1);

				double[] values = feature.getValues();

				double yv; // the y coordinate for the value
				double yvn = y; // the y coordinate of the next value
				double increment = ((double)h) / ((double)values.length);

				int yvi; // yv rounded to the nearest integer
				int yvni = (int)Math.round(yvn); // yvn rounded to the nearest integer

				for (int i=0; i<values.length; i++) {
					g.setColor(colorScale.valueToColor(values[i]));
					yv = yvn;
					yvi = yvni;
					yvn = yv + increment;
					yvni = (int)Math.round(yvn);
					g.fillRect(x1, yvi, w, yvni-yvi+1);
				}

				g.setColor(outlineColor);
				g.drawRect(x1, y, w, h);
			}
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
