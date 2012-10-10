package org.systemsbiology.genomebrowser.model;

import java.util.UUID;

// TODO remove dependency outside model package
import org.systemsbiology.genomebrowser.impl.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;

// The methods for filtering track data are the source of a certain amount of
// angst. It would be nice to create a filter object on the track which could
// efficiently select matching features that could then be iterated over. A
// general purpose means of defining filters is provided by prefuse, which can
// filter tuples nicely. The filters can throw events, so they can be adjusted
// by the ui, and the results propagated to the view. I may switch to that
// approach in the future.

// Should Tracks contain features or merely be a key by which features are
// looked up?


/**
 * A track is a set of related features that will be plotted in
 * relation to their locations on the genome.
 *
 * @author cbare
 */
public interface Track<F extends Feature> {
	public UUID getUuid();
	public String getName();
	public void setName(String name);
	public Attributes getAttributes();

	/**
	 * @return an array of Strands where this track has features
	 */
	public Strand[] strands();

	/**
	 * Note that this iterator may return flyweight object whose state will be
	 * overwritten when the next feature is accessed.
	 * @return an iteratable over all features of the track.
	 * @see Track.featuresAsync
	 */
	public Iteratable<F> features();

	/**
	 * Note that this iterator may return flyweight object whose state will be
	 * overwritten when the next feature is accessed.
	 * @return an iteratable over feature matching the filter.
	 * @see Track.featuresAsync
	 */
	public Iteratable<F> features(FeatureFilter filter);

	/**
	 * Fetch features matching a filter and call a callback function asynchro-
	 * nously. Retrieving features from a data source may take some time.
	 * Retrieving and acting on them asynchronously allows a UI thread to
	 * schedule operations on features.
	 */
	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback);


	public interface Quantitative<Q extends Feature.Quantitative> extends Track<Q> {
//		public Iteratable<Q> features();
//		public Iteratable<Q> features(int blockSize, int start, int end);
		public Range getRange();
	}

	public interface Gene<G extends GeneFeature> extends Track<G> {
//		public Iteratable<G> features();
		public G getFeatureAt(Sequence sequence, Strand strand, int coord);
	}
}
