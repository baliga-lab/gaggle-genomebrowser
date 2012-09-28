package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;
import org.systemsbiology.util.Attributes;


public class IBeamRenderer extends TrackRenderer {
	private int thickness = 1;
	private int staggerInterval = 3;

	@Override
	public void configure(Attributes attr) {
		super.configure(attr);
		thickness = track.getAttributes().getInt("thickness", 1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x1, x2, y;
		int stagger = 0;
		int staggerLimit = 6;
		int previousX2 = -100;
		Strand previousStrand = Strand.none;
		int yTop = (int) (params.getDeviceHeight() * top);
		int yCenter = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height / 2);
		int yBottom = (int) (params.getDeviceHeight() * top + params.getDeviceHeight() * height);
		Stroke s2 = new BasicStroke(thickness);
		Graphics2D g2d = (Graphics2D)g;

		if (outline) {
			int _t = (int) (top * params.getDeviceHeight());
			int _h = (int) (height * params.getDeviceHeight());
			g.setColor(new Color(0x00FF00));
			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
		}

		g.setColor(color);

		for (Feature feature: (Iterable<Feature>)features) {
			x1 = (int) ((feature.getStart() - params.getStart()) * params.getScale());
			x2 = (int) ((feature.getEnd() - params.getStart()) * params.getScale());

			if (previousStrand!=feature.getStrand() || x1 > (previousX2+1)) {
				stagger = 0;
				previousX2 = x2;
			}
			else {
				stagger = (stagger + staggerInterval) % staggerLimit; 
				previousX2 = Math.max(previousX2, x2);
			}
			previousStrand = feature.getStrand();

			// forward strand
			if (feature.getStrand() == Strand.forward) {
				y = yTop + stagger;
			}
			// reverse strand
			else if (feature.getStrand() == Strand.reverse) {
				y = yBottom + stagger;
			}
			// no strand information
			else {
				y = yCenter + stagger;
			}

			Stroke s = g2d.getStroke();
			g2d.setStroke(s2);

			g.drawLine(x1,y,x2,y);
			g.drawLine(x1, y-2, x1, y+2);
			g.drawLine(x2, y-2, x2, y+2);

			g2d.setStroke(s);
		}
	}
}
