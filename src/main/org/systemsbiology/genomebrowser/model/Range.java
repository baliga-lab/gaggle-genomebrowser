package org.systemsbiology.genomebrowser.model;

import java.util.Collection;

/**
 * A range over doubles. I've used it to mean the min and max of a range
 * which implies that the ends are inclusive.
 */
public class Range {
	public final double min;
	public final double max;

	public Range(double min, double max) {
		this.min = Math.min(min, max);
		this.max = Math.max(min, max);
	}

	public Range(Range other) {
		this.min = other.min;
		this.max = other.max;
	}

//	public void setRange(Range range) {
//		this.min = range.min;
//		this.max = range.max;
//	}

	/**
	 * @return the point some percentile of the way between the min and max of the range
	 */
	public double percentileWithinRange(double percent) {
		return (max - min) * percent + min;
	}

	/**
	 * @return takes a percentage from the scale ranging from [range.max, min(range.min,0)] 
	 */
	public double percentileOfMax(double percent) {
		double floor = Math.min(this.min, 0);
		return (max - floor) * percent + floor;
	}

	public String toString() {
		return "[" + min + ", " + max + "]";
	}

	/**
	 * Converts a string of the form 1234, 567890 to
	 * a segment or throws a NumberFormatException.
	 * @param coordinates
	 * @return
	 * @throws NumberFormatException
	 */
	public static Range parse(String coordinates) {
		// split on either a comma, a dash, or whitespace
		String[] coords = coordinates.split("(\\s*,\\s*|\\s*-\\s*|\\s+)");
		if (coords.length > 1) {
			int start = Integer.parseInt(coords[0]);
			int end   = Integer.parseInt(coords[1]);
			return new Range(start, end);
		}
		else {
			throw new NumberFormatException(coordinates);
		}
	}

	public static Range consolidate(Collection<Range> ranges) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (Range range : ranges) {
			if (range.min < min) min = range.min;
			if (range.max > max) max = range.max;
		}
		return new Range(min, max);
	}
}
