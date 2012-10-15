package org.systemsbiology.genomebrowser.visualization;

import java.awt.Color;
import static java.lang.Math.*;
import org.systemsbiology.genomebrowser.model.Range;


public class GreyColorScale implements ColorScale {
	Range range = new Range(-1.0, 1.0);
	double gamma = 0.8;

	public void setRange(Range range) {
		this.range = range;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public Color valueToColor(double value) {
		value = max(min(value, range.max), range.min) / (range.max - range.min);
		int level = (int)(255.0 * pow(value, gamma));
		return new Color(level, level, level);
	}
}
