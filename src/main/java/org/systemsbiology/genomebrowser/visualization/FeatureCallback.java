package org.systemsbiology.genomebrowser.visualization;

import org.systemsbiology.genomebrowser.impl.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.util.Iteratable;


public class FeatureCallback implements AsyncFeatureCallback {
	private final RenderingContext context;


	public FeatureCallback(RenderingContext context) {
		this.context = context;
	}

	public void consumeFeatures(final Iteratable<? extends Feature> features, FeatureFilter filter) {
		if (context.frame != context.scheduler.getCurrentFrame()) return;
		context.renderer.draw(context.getGraphics(), features, filter.strand);
	}
}
