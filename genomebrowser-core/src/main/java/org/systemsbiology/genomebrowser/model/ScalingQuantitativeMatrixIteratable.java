package org.systemsbiology.genomebrowser.model;

import java.util.Iterator;
import org.systemsbiology.util.Iteratable;
import org.systemsbiology.util.MathUtils;

/**
 * Wraps an iterable of quantitative matrix features in order to provide scaling.
 * 
 * Copied from ScalingQuantitativeIteratable and customized for matrix tracks
 * 
 * The result is an Iteratable of scaled quantitative features which have
 * a value (average or some measure of central tendency) and a min and
 * max to allow the range of values to be drawn.
 * 
 * Used by ScalingTrackRenderer.
 * @author cbare
 */
public class ScalingQuantitativeMatrixIteratable implements Iteratable<Feature.ScaledQuantitative> {
    Iterator<Feature.Matrix> iterator;
    int blockSize;
    ScaledQuantitativeFeature feature = new ScaledQuantitativeFeature();


    /**
     * @param iterable of quantitative features.
     */
    public ScalingQuantitativeMatrixIteratable(Iterable<Feature.Matrix> iterable, int blockSize) {
        this.iterator = iterable.iterator();
        this.blockSize = blockSize;
    }

    /**
     * @param iterable of quantitative features.
     */
    public ScalingQuantitativeMatrixIteratable(Iterator<Feature.Matrix> iterator, int blockSize) {
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
        Feature.Matrix fm = iterator.next();
        double sum = 0.0;
        double min = fm.getValue();
        double max = fm.getValue();
        int end = fm.getEnd();
        int count=0;

        feature.start = fm.getStart();
        feature.seqId = fm.getSeqId();
        feature.strand = fm.getStrand();

        for (double value : fm.getValues()) {
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
            count++;
        }

        for (int i=1; i<blockSize && iterator.hasNext(); i++) {
            fm = iterator.next();

            for (double value : fm.getValues()) {
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
                count++;
            }
            end = Math.max(end, fm.getEnd());
            if (feature.strand != fm.getStrand() || !feature.seqId.equals(fm.getSeqId()))
                break;
        }
        feature.set(end, min, max, sum/count);
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
