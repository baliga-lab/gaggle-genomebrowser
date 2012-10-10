package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;
import org.systemsbiology.genomebrowser.util.Attributes;


public class HorizontalLineRenderer extends TrackRenderer {
	private int thickness = 2;
	private int offset;

	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		thickness = track.getAttributes().getInt("thickness", 2);
		offset = track.getAttributes().getInt("offset", -50000);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2, y;
		int yCenter = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
		int yTop, yBottom;
		Stroke s2 = new BasicStroke(thickness);
		Graphics2D g2d = (Graphics2D)g;

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x00FF00));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

		// compute y coordinate as offset from the center,
		// if offset attribute is specified,
		// otherwise derive it from top and height.
		if (offset > -50000) {
			yTop = yCenter - offset;
			yBottom = yCenter + offset;
		}
		else {
			yTop = (int) (params.getDeviceHeight() * top);
			yBottom = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height);
		}

		g.setColor(color);

		for (Feature feature: (Iterable<Feature>)features) {
			x1 = (int) ((feature.getStart() - params.getStart()) * params.getScale());
			x2 = (int) ((feature.getEnd() - params.getStart()) * params.getScale());

			// forward strand
			if (feature.getStrand() == Strand.forward) {
				y = yTop;
			}
			// reverse strand
			else if (feature.getStrand() == Strand.reverse) {
				y = yBottom;
			}
			// no strand information
			else {
				y = yCenter;
			}

			Stroke s = g2d.getStroke();
			g2d.setStroke(s2);

			g.drawLine(x1,y,x2,y);

			g2d.setStroke(s);
		}
	}
}
