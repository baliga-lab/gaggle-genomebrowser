package org.systemsbiology.genomebrowser.impl;

import java.util.Iterator;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;


/**
 * Wraps an iterable of quantitative features in order to provide scaling.
 * Scaling = multiple data points are compressed into a single data point,
 * which makes sense when the resolution of data points is higher than
 * the resolution of our output device.
 * 
 * The result is an Iteratable of scaled quantitative features which have
 * a value (average or some measure of central tendency) and a min and
 * max to allow the range of values to be drawn.
 * 
 * Used by ScalingTrackRenderer.
 * 
 * @author cbare
 */
public class ScalingQuantitativeIteratable implements Iteratable<Feature.ScaledQuantitative> {
	Iterator<Feature.Quantitative> iterator;
	int blockSize;
	ScaledQuantitativeFeature feature = new ScaledQuantitativeFeature();


	/**
	 * @param iterable of quantitative features.
	 */
	public ScalingQuantitativeIteratable(Iterable<Feature.Quantitative> iterable, int blockSize) {
		this.iterator = iterable.iterator();
		this.blockSize = blockSize;
	}

	/**
	 * @param iterable of quantitative features.
	 */
	public ScalingQuantitativeIteratable(Iterator<Feature.Quantitative> iterator, int blockSize) {
		this.iterator = iterator;
		this.blockSize = blockSize;
	}
	

	public Iterator<Feature.ScaledQuantitative> iterator() {
		return this;
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	/**
	 * @return a flyweight scaled quantitative feature. Repeated invocations
	 * return the same instance, with different values.
	 */
	public Feature.ScaledQuantitative next() {
		Feature.Quantitative fq = iterator.next();
		double sum = fq.getValue();
		double min = sum;
		double max = sum;
		int end = fq.getEnd();
		feature.start = fq.getStart();
		feature.seqId = fq.getSeqId();
		feature.strand = fq.getStrand();
		for (int i=1; i<blockSize && iterator.hasNext(); i++) {
			fq = iterator.next();
			sum += fq.getValue();
			min = Math.min(min, fq.getValue());
			max = Math.max(max, fq.getValue());
			end = Math.max(end, fq.getEnd());
			if (feature.strand != fq.getStrand() || !feature.seqId.equals(fq.getSeqId()))
				break;
		}
		feature.set(end, min, max, sum/blockSize);
		return feature;
	}

	public void remove() {
		throw new UnsupportedOperationException("Can't remove");
	}

	private static class ScaledQuantitativeFeature implements Feature.ScaledQuantitative {
		String seqId;
		Strand strand;
		double min, max, value;
		int start, end;

		public void set(int end, double min, double max, double value) {
			this.end = end;
			this.min = min;
			this.max = max;
			this.value = value;
		}

		public double getMax() {
			return max;
		}

		public double getMin() {
			return min;
		}

		public double getValue() {
			return value;
		}

		public int getCentralPosition() {
			return MathUtils.average(start, end);
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		public String getLabel() {
			return String.valueOf(value);
		}

		public String getSeqId() {
			return seqId;
		}

		public Strand getStrand() {
			return strand;
		}
		
	}

}
