package org.systemsbiology.genomebrowser.sqlite;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Block;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;


/**
 * A block of features with a single coordinate and a quantitative measurement and a p-value.
 * @author cbare
 */
public class PositionalQuantitativePvalueBlock implements Block<Feature.QuantitativePvalue> {
	private final BlockKey key;
	private int[] positions;
	private double[] values;
	private double[] pvalues;


	public PositionalQuantitativePvalueBlock(BlockKey key, int[] positions, double[] values, double[] pvalues) {
		this.key = key;
		this.positions = positions;
		this.values = values;
		this.pvalues = pvalues;
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
	public Iterator<Feature.QuantitativePvalue> iterator() {
		return features();
	}

	/**
	 * @return iterator of flyweight quantitative features
	 */
	public Iteratable<Feature.QuantitativePvalue> features() {
		return new FeaturesIteratable();
	}

	/**
	 * @return iterator of flyweight quantitative features
	 */
	public Iteratable<Feature.QuantitativePvalue> features(int start, int end) {
		return new WindowedFeaturesIteratable(start, end);
	}

	class FeaturesIteratable implements Iteratable<Feature.QuantitativePvalue> {
		FlyweightFeature feature = new FlyweightFeature();
		int last = positions.length - 1;
		int next;

		public boolean hasNext() {
			return next < last;
		}

		public Feature.QuantitativePvalue next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<Feature.QuantitativePvalue> iterator() {
			return this;
		}		
	}

	class WindowedFeaturesIteratable implements Iteratable<Feature.QuantitativePvalue> {
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

		public Feature.QuantitativePvalue next() {
			feature.i = next++;
			return feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<Feature.QuantitativePvalue> iterator() {
			return this;
		}		
	}

	class FlyweightFeature implements Feature.QuantitativePvalue {
		int i;

		public double getValue() {
			return values[i];
		}
		
		public double getPvalue() {
			return pvalues[i];
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
			return String.format("%.3f (pval=%.3f)", values[i], pvalues[i]);
		}
		
		public String toString() {
			return String.format("(Feature: %s%s:%d %.2f)", getSeqId(), getStrand().toAbbreviatedString(), getCentralPosition(), getValue()); 
		}
	}
}
