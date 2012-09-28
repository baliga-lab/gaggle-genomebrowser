package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.util.Iteratable;

/**
 * The contract of a class that implements AsyncFeatureCallback is that
 * features come back after some delay, and possibly in more than one piece.
 * So, consumeFeatures() may be called more than once.
 */
public interface AsyncFeatureCallback {
	/**
	 * @param features an iterator of possibly flyweight features
	 * @param filter
	 */
	public void consumeFeatures(Iteratable<? extends Feature> features, FeatureFilter filter);
}


/*

Looking at this later, this seems ridiculously over-complicated. I think I've put
some things at the wrong level of abstraction. The asynchrony is needed due to the
fact that we get our features in blocks, which may come at different times. That's
an implementation detail that certainly doesn't belong baked into the way the UI
calls renderers. A better way to think about it would be that the UI tells the
renderers to redraw and passes in the viewport. Plugging the features into the
renderer should happen elsewhere. Dragging that part up into the UI is whacked.

In other words, the UI shouldn't push features into the renderer, the renderer
should pull features from a source. A renderer might get features from a weird
source, or select sources based on the viewport (for example, showing motifs
present in the partiular view for several sparsely populated sources of motifs).

For that matter, tracks are a little messed up. A track conflates two things
which should be separate, a container of features and a place to draw them on the
screen (a "lane").
*/
