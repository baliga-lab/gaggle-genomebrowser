package org.systemsbiology.genomebrowser.model;

import java.util.ArrayList;
import java.util.List;


/**
 * A range of coordinates on a specific sequence and strand. Used to test
 * features and blocks of features for intersection with the area of the
 * genome visible on the screen.
 * @author cbare
 */
public class FeatureFilter {
	final public Sequence sequence;
	final public Strand strand;
	final public int start;
	final public int end;

	public FeatureFilter(Sequence sequence, Strand strand, int start, int end) {
		this.sequence = sequence;
		this.strand = strand;
		this.start = start;
		this.end = end;
	}

	public FeatureFilter(Sequence sequence, Strand strand) {
		this.sequence = sequence;
		this.strand = strand;
		this.start = Integer.MIN_VALUE;
		this.end = Integer.MAX_VALUE;
	}

	public FeatureFilter(Sequence sequence, int start, int end) {
		this.sequence = sequence;
		this.strand = Strand.any;
		this.start = start;
		this.end = end;
	}

	public FeatureFilter(Sequence sequence) {
		this.sequence = sequence;
		this.strand = Strand.any;
		this.start = Integer.MIN_VALUE;
		this.end = Integer.MAX_VALUE;
	}

	public boolean passes(Feature feature) {
		return (feature.getSeqId().equals(sequence.getSeqId()))
			&& (strand==Strand.any || strand==feature.getStrand())
			&& (feature.getEnd() >= start)
			&& (feature.getStart() <= end);
	}

	public <T extends Feature> List<T> apply(List<T> input) {
		List<T> output = new ArrayList<T>();
		for (T feature: input) {
			if (passes(feature)) {
				output.add(feature);
			}
		}
		return output;
	}

	public boolean overlaps(FeatureFilter other) {
		if (!this.sequence.equals(other.sequence)) return false;
		if (this.strand != Strand.any && other.strand != Strand.any && this.strand != other.strand) return false;
		return (this.start <= other.end && this.end >= other.start);
	}

	public boolean overlaps(Sequence sequence, Strand strand, int coord) {
		if (!this.sequence.equals(sequence)) return false;
		if (this.strand != Strand.any && strand != Strand.any && this.strand != strand) return false;
		return (this.start <= coord && this.end >= coord);
	}

	public String toString() {
		return String.format("[FeatureFilter: %s, %s, %d-%d]", sequence.getSeqId(), strand.toAbbreviatedString(), start, end);
	}

	@Override
	public int hashCode() {
		int result = 3911 + end;
		result = 241 * result + sequence.getUuid().hashCode();
		result = 31 * result + start;
		result = 5 * result + ((strand == null) ? 0 : strand.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FeatureFilter other = (FeatureFilter) obj;
		if (sequence == null) {
			if (other.sequence != null)
				return false;
		}
		else if (!sequence.equals(other.sequence))
			return false;
		if (strand == null) {
			if (other.strand != null)
				return false;
		}
		else if (!strand.equals(other.strand))
			return false;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public static final FeatureFilter CLEAR = new FeatureFilter(null, null);
}
