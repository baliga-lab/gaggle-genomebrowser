package org.systemsbiology.genomebrowser.impl;

import java.util.Iterator;
import java.util.List;

import org.systemsbiology.genomebrowser.model.BlockEntry;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Feature.Quantitative;
import org.systemsbiology.util.Iteratable;

/**
 * For use with in-memory tracks that organize their features into blocks.
 * @author cbare
 * @see QuantitativeTrack
 */
class BlockIteratable implements Iteratable<Feature.Quantitative> {
	private List<BlockEntry<Feature.Quantitative>> blocks;
	private FeatureFilter filter;
	private int b;
	private Iterator<Feature.Quantitative> features;

	public BlockIteratable(List<BlockEntry<Feature.Quantitative>> blocks, FeatureFilter filter) {
		this.blocks = blocks;
		this.filter = filter;
	}

	public boolean hasNext() {
		while ((features==null || !features.hasNext()) && b < blocks.size()) {
			BlockEntry<Feature.Quantitative> entry = blocks.get(b++);
			if (filter.overlaps(entry.key)) {
				features = entry.block.features(filter.start, filter.end);
			}
		}
		return features==null ? false : features.hasNext();
	}

	public Feature.Quantitative next() {
		return features==null ? null : features.next();
	}

	public void remove() {
		throw new UnsupportedOperationException("remove not supported");
	}

	public Iterator<Quantitative> iterator() {
		return this;
	}
}
