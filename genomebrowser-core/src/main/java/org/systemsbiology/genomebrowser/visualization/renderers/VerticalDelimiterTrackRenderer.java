package org.systemsbiology.genomebrowser.visualization.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_ROUND;



/**
 * A track that plots data as a vertical delimiter. Useful for start/end locations.
 */
public class VerticalDelimiterTrackRenderer extends QuantitativeTrackRenderer {
	float weight = 1.0f;
	float[] dashes;
	float dashPhase;

	public VerticalDelimiterTrackRenderer() {
		color = new Color(0x80000000);
	}

	public void configure(Attributes attr) {
		super.configure(attr);
		if (attr.containsKey("weight")) {
			this.weight = attr.getFloat("weight", 1.0f);
		}
		
		if ("dash".equals(attr.getString("style", "dash"))) {
			dashes = new float[] {9.0f, 5.0f};
		}
		else if ("dash.dot".equals(attr.getString("style"))) {
			dashes = new float[] {9.0f, 5.0f, 1.0f, 5.0f};
		}
		else if ("long.dash".equals(attr.getString("style"))) {
			dashes = new float[] {19.0f, 5.0f};
		}
		else if ("short.dash".equals(attr.getString("style"))) {
			dashes = new float[] {3.0f, 3.0f};
		}
		else if ("solid".equals(attr.getString("style"))) {
			dashes = null;
		}
		
		dashPhase = attr.getFloat("dash.phase", 0.0f);
	}

	@Override
	public void draw(Graphics g, Iterable<? extends Feature> features, Strand strand) {
		int x;
		int y0 = (int) (params.getDeviceHeight() * top);
		int y1 = (int) (y0 + params.getDeviceHeight() * height);

		g.setColor(color);
        Graphics2D g2d = (Graphics2D)g;
        Stroke oldStroke = g2d.getStroke();
        
        // BasicStroke(float width, int cap, int join, 
        // float miterlimit, float[] dash, float dash_phase)
        if (dashes==null)
        	g2d.setStroke(new BasicStroke(weight, CAP_BUTT, JOIN_ROUND));
        else
            g2d.setStroke(new BasicStroke(weight, CAP_BUTT, JOIN_ROUND, 0.0f, dashes, dashPhase));

		@SuppressWarnings("unchecked")
		Iterable<Feature.Quantitative> quantitativeFeatures = (Iterable<Feature.Quantitative>)features;
		for (Feature.Quantitative feature: quantitativeFeatures) {
			x = params.toScreenX(feature.getCentralPosition());
			g.drawLine(x, y0, x, y1);
		}
        g2d.setStroke(oldStroke);
	}
}
