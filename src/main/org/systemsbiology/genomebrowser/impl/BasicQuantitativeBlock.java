package org.systemsbiology.genomebrowser.impl;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;


public class BasicQuantitativeBlock<F extends Feature.Quantitative> implements Block<F> {
	private final Sequence sequence;
	private final Strand strand;
	private final int[] starts;
	private final int[] ends;
	private final double[] values;


	public BasicQuantitativeBlock(Sequence sequence, Strand strand, int[] starts, int[] ends, double[] values) {
		this.sequence = sequence;
		this.strand = strand;
		this.starts = starts;
		this.ends = ends;
		this.values = values;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public Strand getStrand() {
		return strand;
	}

	public Iteratable<F> features() {
		return new FeaturesIteratable();
	}

	public Iteratable<F> features(int start, int end) {
		return new WindowedFeaturesIteratable(start, end);
	}

	public Iterator<F> iterator() {
		return features();
	}

	private class FlyweightQuantitativeFeature implements Feature.Quantitative {
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
			return sequence.getSeqId();
		}

		public int getStart() {
			return starts[i];
		}

		public Strand getStrand() {
			return strand;
		}
		
		public String toString() {
			return String.format("(Feature: %s, %s, %d, %d, %.2f)", getSeqId(), getStrand(), starts[i], ends[i], values[i]); 
		}

		public String getLabel() {
			return String.format("%.2f", values[i]);
		}
	}

	class FeaturesIteratable implements Iteratable<F> {
		FlyweightQuantitativeFeature feature = new FlyweightQuantitativeFeature();
		int len = starts.length;
		int next;

		public boolean hasNext() {
			return next < len;
		}

		@SuppressWarnings("unchecked")
		public F next() {
			feature.i = next++;
			return (F)feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<F> iterator() {
			return this;
		}		
	}

	class WindowedFeaturesIteratable implements Iteratable<F> {
		FlyweightQuantitativeFeature feature = new FlyweightQuantitativeFeature();
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

		@SuppressWarnings("unchecked")
		public F next() {
			feature.i = next++;
			return (F)feature;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove() not supported.");
		}

		public Iterator<F> iterator() {
			return this;
		}		
	}

}
