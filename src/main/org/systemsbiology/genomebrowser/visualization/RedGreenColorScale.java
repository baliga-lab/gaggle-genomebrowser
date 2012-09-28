package org.systemsbiology.genomebrowser.visualization;

import java.awt.Color;

import org.systemsbiology.genomebrowser.model.Range;


/**
 * use heatmap coloring: red = hot, green = cold
 */
public class RedGreenColorScale implements ColorScale {
	Range range = new Range(-1.0, 1.0);
	double gamma = 0.8;
	
	public void setRange(Range range) {
		this.range = range;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public Color valueToColor(double value) {
		if (value >=0) {
			// red = increased
			double x = Math.pow(value/range.max, gamma);
			return new Color( Math.min(255, (int)(255.0 * x)), 00, 00);
		}
		else {
			// green = decreased
			double x = Math.pow(value/range.min, gamma);
			return new Color( 00, Math.min(255, (int)(255.0 * x)), 00);
		}
	}

}
