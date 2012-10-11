package org.systemsbiology.genomebrowser.sqlite;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Block;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;

/**
 * A contiguous block of quantitative features on the same sequence and strand.
 * @author cbare
 */
public class SegmentBlock implements Block<Feature.Quantitative> {
	private final BlockKey key;
	private final int[] starts;
	private final int[] ends;
	private final double[] values;


	public SegmentBlock(BlockKey key, int[] starts, int[] ends, double[] values) {
		this.key = key;
		this.starts = starts;
		this.ends = ends;
		this.values = values;
	}

	public Sequence getSequence() {
		// TODO SegmentBlock.getSequence - key only has seqId
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
		int len = starts.length;
		int next;

		public boolean hasNext() {
			return next < len;
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
		int len = starts.length;
		int start;
		int end;
		int next;

		public WindowedFeaturesIteratable(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean hasNext() {
			// features are sorted by start,end
			while (next < len && ends[next] < start) {
				next++;
			}
			return (next < len) && starts[next] < end;
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
			return MathUtils.average(starts[i], ends[i]);
		}

		public int getEnd() {
			return ends[i];
		}

		public String getSeqId() {
			return key.getSeqId();
		}

		public int getStart() {
			return starts[i];
		}

		public Strand getStrand() {
			return key.getStrand();
		}
		
		public String toString() {
			return String.format("(Feature: %s, %s, %d, %d, %.2f)", getSeqId(), getStrand(), starts[i], ends[i], values[i]); 
		}

		public String getLabel() {
			return null;
		}
	}
}
