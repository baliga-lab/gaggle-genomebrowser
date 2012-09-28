package org.systemsbiology.genomebrowser.visualization.tracks.renderers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import static java.lang.Math.*;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.ui.HasTooltips;
import org.systemsbiology.genomebrowser.visualization.tracks.TrackRenderer;
import org.systemsbiology.util.Attributes;


/**
 * A track renderer that plots a triangular marker in some color.
 */
public class TriangleMarkerRenderer extends TrackRenderer implements HasTooltips {
	GeneralPath triangle;

	@Override
	public void configure(Attributes attr) {
		this.color = Color.RED;
		super.configure(attr);
		triangle = new GeneralPath();
		triangle.moveTo(0f, 0f);
		float y = (float)sin(PI/3.0);
		triangle.lineTo(-0.5f, y);
		triangle.lineTo(0.5f, y);
		triangle.lineTo(0f, 0f);
		int size = attr.getInt("triangle.size", 10);
		triangle.transform(AffineTransform.getScaleInstance(size, size));
//		outline = true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x;
		int y0 = (int) (params.getDeviceHeight() * top);

//        if (outline) {
//			int _t = (int) (top * params.getDeviceHeight());
//			int _h = (int) (height * params.getDeviceHeight());
//			g.setColor(new Color(0x0066FF));
//			g.drawRect(0, _t+1, params.getDeviceWidth()-1, _h-1);
//        }

		Color oldColor = g.getColor();
        g.setColor(color);

		for (Feature feature: (Iterable<Feature>)features) {
        	x = params.toScreenX(feature.getCentralPosition());
			((Graphics2D)g).fill(triangle.createTransformedShape(AffineTransform.getTranslateInstance(x, y0)));
        }
		
		g.setColor(oldColor);
	}

	public String getTooltip(int x, int y) {
		Feature feature = getFeatureAt(x,y);
		if (feature==null) return null;
		if (track.getAttributes().containsKey("label"))
			return track.getAttributes().getString("label") + " " + feature.getLabel();
		return feature.getLabel();
	}
	
	private Feature getFeatureAt(int x, int y) {
		int yr = y - (int) (params.getDeviceHeight() * top);
		if (yr < 0 || yr > triangle.getBounds().height) return null; 
		int p = params.toGenomeCoordinate(x);
		int w = (int)ceil(triangle.getBounds().width / 2.0 / params.getScale());
		for (Feature feature : track.features(new FeatureFilter(params.getSequence(), Strand.any, params.getStart(), params.getEnd()))) {
			if (abs(feature.getCentralPosition() - p) <= w) {
				return feature;
			}
		}
		return null;
	}
}
