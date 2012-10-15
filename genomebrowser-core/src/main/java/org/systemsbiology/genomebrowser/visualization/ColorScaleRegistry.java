package org.systemsbiology.genomebrowser.visualization;

import java.util.HashMap;
import java.util.Map;


/**
 * Allow clients to look up ColorScale variants by name.
 */
public class ColorScaleRegistry {
	Map<String, Class<? extends ColorScale>> map = new HashMap<String, Class<? extends ColorScale>>();
	
	public void put(String name, Class<? extends ColorScale> colorScaleClass) {
		map.put(name, colorScaleClass);
	}

	public ColorScale get(String name) {
		try {
			return map.get(name).newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to create color scale of type: \"" + name + "\"");
		}
	}

	public void init() {
		put("red.green", RedGreenColorScale.class);
		put("blue.yellow", BlueYellowColorScale.class);
		put("grey", GreyColorScale.class);
	}
}
