package org.systemsbiology.genomebrowser.visualization;

import java.awt.Color;

import org.systemsbiology.genomebrowser.model.Range;


/**
 * A strategy for converting a measured value to a color for rendering.
 * 
 * @see ColorScaleRegistry
 */
public interface ColorScale {
	public Color valueToColor(double value);
	public void setRange(Range range);
	public void setGamma(double gamma);
}
