package org.systemsbiology.genomebrowser.impl;

import java.util.Iterator;
import java.util.List;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.util.Iteratable;


/**
 * Iterates the subset of a list of features that falls inside the range
 * given by the start and end constructor parameters.
 */
public class FeatureIteratable<F extends Feature> implements Iteratable<F> {
	int len;
	int next;
	int start;
	int end;
	List<F> features;

	public FeatureIteratable(List<F> features, int start, int end) {
		this.features = features;
		this.start = start;
		this.end = end;
		this.len = features==null ? 0 : features.size();
	}

	public boolean hasNext() {
		while (next < len && features.get(next).getEnd() < start) {
			next++;
		}
		return (next < len) && features.get(next).getStart() < end;
	}

	public F next() {
		int i = next++;
		return features.get(i);
	}

	public void remove() {
		throw new UnsupportedOperationException("remove not supported in feature iterators");
	}

	public Iterator<F> iterator() {
		return this;
	}
}
