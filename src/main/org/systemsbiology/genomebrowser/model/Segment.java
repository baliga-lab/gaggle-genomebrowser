package org.systemsbiology.genomebrowser.model;


/**
 * A sequence name, Start, End tuple after GFF. A segment of a particular chromosome
 * with start and end coordinates where start <= end.
 * @author cbare
 */
public class Segment extends Interval {
	public final String seqId;


	/**
	 * Create a segment while ensuring that start <= end.
	 */
	public Segment(String seqId, int start, int end) {
		super(start, end);
		this.seqId = seqId;
	}

	public boolean overlaps(Segment other) {
		return this.seqId.equals(other.seqId) &&
			   super.overlaps(other);
	}

	public Segment expandToInclude(Segment other) {
		if (!this.seqId.equals(other.seqId))
			return this;
		// we're assuming that start < end for both this and other
		return new Segment(seqId, Math.min(start, other.start), Math.max(end, other.end));
	}

	/**
	 * returns a new segment which is trimmed by removing the given segment from
	 * either end of this segment. If this segment entirely encloses segment, we
	 * just return this segment, since we don't want to create a segment with a 
	 * hole in it. (Or do we? Which is more natural?) If the given segment
	 * encloses this segment, return null.
	 */
	public Segment trimOverlap(Segment segment) {
		if (!this.seqId.equals(segment.seqId))
			return this;

		// if other completely covers this segment, return null
		if (segment.start <= this.start && segment.end >= this.end)
		return null;

		// if other overlaps start of this segment
		if (segment.start <= this.start && segment.end >= this.start)
			return new Segment(seqId, segment.end+1, this.end);

		// if other overlaps end of this segment
		if (segment.start <= this.end && segment.end >= this.end)
			return new Segment(seqId, this.start, segment.start-1);
		
		return this;
	}

	public String toString() {
		return "[\"" + seqId + "\", " + start + ", " + end + "]";
	}


	/**
	 * Converts a string of the form 1234, 567890 to
	 * a segment or throws a NumberFormatException.
	 * @param coordinates
	 * @return
	 * @throws NumberFormatException
	 */
	public static Segment parse(String name, String coordinates) {
		// split on either a comma, a dash, or whitespace
		String[] coords = coordinates.split("(\\s*,\\s*|\\s*-\\s*|\\s+)");
		if (coords.length > 1) {
			int start = Integer.parseInt(coords[0]);
			int end   = Integer.parseInt(coords[1]);
			return new Segment(name, start, end);
		}
		else {
			throw new NumberFormatException(coordinates);
		}
	}

	public static Segment fromFeature(Feature feature) {
		return new Segment(feature.getSeqId(), feature.getStart(), feature.getEnd());
	}
}
