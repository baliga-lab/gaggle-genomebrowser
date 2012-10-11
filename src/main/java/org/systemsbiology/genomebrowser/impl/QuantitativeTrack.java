package org.systemsbiology.genomebrowser.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Block;
import org.systemsbiology.genomebrowser.model.BlockEntry;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.AsyncFeatureCallback;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MultiIteratable;

/**
 * A basic but inefficient implementation of Track.Quantitative mainly for use in testing.
 * 
 * @author cbare
 */
public class QuantitativeTrack implements Track.Quantitative<Feature.Quantitative> {
	private final UUID id;
	private String name;
	private final Attributes attr = new Attributes();
	private List<BlockEntry<Feature.Quantitative>> blocks = new ArrayList<BlockEntry<Feature.Quantitative>>();


	public QuantitativeTrack(String name) {
		this.id = UUID.randomUUID();
		this.name = name;
	}

	public QuantitativeTrack(UUID uuid, String name) {
		this.id = uuid;
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public void putFeatures(FeatureFilter key, Block<? extends Feature.Quantitative> block) {
		blocks.add(new BlockEntry<Feature.Quantitative>(key, (Block<Feature.Quantitative>)block));
	}

	public UUID getUuid() {
		return id;
	}

	public Attributes getAttributes() {
		return attr;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Strand[] strands() {
		Set<Strand> strands = new HashSet<Strand>();
		for (Feature feature: features()) {
			strands.add(feature.getStrand());
		}
		return strands.toArray(new Strand[strands.size()]);
	}

	public Range getRange() {
		// TODO implement QuantitativeTrack.getRange()
		return new Range(-1.0, 1.0);
	}

	public void featuresAsync(FeatureFilter filter, AsyncFeatureCallback callback) {
		for (BlockEntry<Feature.Quantitative> entry : blocks) {
			if (filter.overlaps(entry.key)) {
				callback.consumeFeatures(entry.block.features(filter.start, filter.end), entry.key);
			}
		}
	}

	public Iteratable<Feature.Quantitative> features() {
		List<Block<Feature.Quantitative>> blocks = new ArrayList<Block<Feature.Quantitative>>(); 
		for (BlockEntry<Feature.Quantitative> entry : this.blocks) {
			blocks.add(entry.block);
		}
		return new MultiIteratable<Feature.Quantitative>(blocks);
	}

	public Iteratable<Feature.Quantitative>features(FeatureFilter filter) {
		return new BlockIteratable(blocks, filter);
	}

	public Iteratable<Feature.Quantitative> features(int blockSize, int start, int end) {
		// TODO implement scaling of quantitative features
		return null;
	}
}


/*
class QuantitativeFeatureBlock implements Block<Feature.Quantitative> {
	String label;
	Sequence sequence;
	Strand strand;
	int[] starts;
	int[] ends;
	double[] values;

	public Sequence getSequence() {
		return sequence;
	}

	public Strand getStrand() {
		return strand;
	}

	public Iteratable<Feature.Quantitative> features() {
		return new Iter();
	}

	public Iteratable<Feature.Quantitative> features(int start, int end) {
		return new LimitIter(start, end);
	}

	public Iterator<Feature.Quantitative> iterator() {
		return features();
	}


	class FlyweightQuantitativeFeature implements Feature.Quantitative {
		int i;

		public double getValue() {
			return values[i];
		}

		public int getStart() {
			return starts[i];
		}

		public int getEnd() {
			return ends[i];
		}

		public int getCentralPosition() {
			return (starts[i] + ends[i]) >>> 1;
		}

		public String getLabel() {
			return null;
		}

		public String getSeqId() {
			return null;
		}

		public Strand getStrand() {
			return null;
		}
		
	}

	class Iter implements Iteratable<Feature.Quantitative> {
		int next;
		FlyweightQuantitativeFeature feature = new FlyweightQuantitativeFeature();

		public int length() {
			return starts.length;
		}

		public boolean hasNext() {
			return (next < length());
		}

		public Feature.Quantitative next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove not implemented");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}
	}

	class LimitIter implements Iteratable<Feature.Quantitative> {
		int start;
		int end;
		int next;
		FlyweightQuantitativeFeature feature = new FlyweightQuantitativeFeature();

		public LimitIter(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int length() {
			return starts.length;
		}

		public boolean hasNext() {
			while ((next < starts.length) && (ends[next] < start))
					next++;
			return (next < starts.length) && (starts[next] < end);
		}

		public Feature.Quantitative next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove not implemented");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}
	}
}
*/
