package org.systemsbiology.genomebrowser.sqlite;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.impl.Block;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;


/**
 * A block of features with a single coordinate and a quantitative measurement.
 * @author cbare
 */
public class PositionalBlock implements Block<Feature.Quantitative> {
	private final BlockKey key;
	private int[] positions;
	private double[] values;


	public PositionalBlock(BlockKey key, int[] positions, double[] values) {
		this.key = key;
		this.positions = positions;
		this.values = values;
	}

	public Sequence getSequence() {
		// TODO get Sequence from PositionalBlock
		return null;
	}

	public Strand getStrand() {
		return key.getStrand();
	}

	/**
	 * @return iterator of flyweight quantitative features
	 */
	public Iterator<Feature.Quantitative> iterator() {
		return features();
	}

	/**
	 * @return iterator of flyweight quantitative features
	 */
	public Iteratable<Feature.Quantitative> features() {
		return new FeaturesIteratable();
	}

	/**
	 * @return iterator of flyweight quantitative features
	 */
	public Iteratable<Feature.Quantitative> features(int start, int end) {
		return new WindowedFeaturesIteratable(start, end);
	}

	class FeaturesIteratable implements Iteratable<Feature.Quantitative> {
		FlyweightFeature feature = new FlyweightFeature();
		int last = positions.length - 1;
		int next;

		public boolean hasNext() {
			return next < last;
		}

		public Feature.Quantitative next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}		
	}

	class WindowedFeaturesIteratable implements Iteratable<Feature.Quantitative> {
		FlyweightFeature feature = new FlyweightFeature();
		int start;
		int end;
		int next;

		public WindowedFeaturesIteratable(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean hasNext() {
			// features are sorted by start,end
			while (next < positions.length && positions[next] < start) {
				next++;
			}
			return (next < positions.length) && positions[next] < end;
		}

		public Feature.Quantitative next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<Feature.Quantitative> iterator() {
			return this;
		}		
	}

	class FlyweightFeature implements Feature.Quantitative {
		int i;

		public double getValue() {
			return values[i];
		}

		public int getCentralPosition() {
			return positions[i];
		}

		public int getEnd() {
			return positions[i];
		}

		public String getSeqId() {
			return key.getSeqId();
		}

		public int getStart() {
			return positions[i];
		}

		public Strand getStrand() {
			return key.getStrand();
		}

		public String getLabel() {
			return String.format("%.3f", values[i]);
		}
		
		public String toString() {
			return String.format("(Feature: %s%s:%d %.2f)", getSeqId(), getStrand().toAbbreviatedString(), getCentralPosition(), getValue()); 
		}
	}
}
