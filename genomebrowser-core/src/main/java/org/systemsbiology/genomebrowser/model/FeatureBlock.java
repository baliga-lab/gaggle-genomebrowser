package org.systemsbiology.genomebrowser.model;

import java.util.Iterator;
import java.util.List;

import org.systemsbiology.util.Iteratable;

/**
 * A simple implementation of Block backed by a List of features for use when
 * holding in memory all features in a track is desirable for performance and
 * not prohibitively large. (Gene tracks.)
 */
public class FeatureBlock<F extends Feature> implements Block<F> {
	private Sequence sequence;
	private Strand strand;
	private List<F> features;

	public FeatureBlock(Sequence sequence, Strand strand, List<F> features) {
		this.sequence = sequence;
		this.strand = strand;
		this.features = features;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public Strand getStrand() {
		return strand;
	}

	public Iteratable<F> features() {
		return new Iteratable.Wrapper<F>(features.iterator());
	}

	public Iteratable<F> features(int start, int end) {
		return new FeatureIteratable<F>(features, start, end);
	}

	public Iterator<F> iterator() {
		return features.iterator();
	}
}
