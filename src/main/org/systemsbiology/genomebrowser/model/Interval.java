package org.systemsbiology.genomebrowser.model;



/**
 * A pair of start and end coordinates where start <= end.
 * @author cbare
 */
public class Interval {
	public final int start;
	public final int end;


	/**
	 * Create a segment while ensuring that start <= end.
	 */
	public Interval(int start, int end) {
		this.start = Math.min(start, end);
		this.end = Math.max(start, end);
	}

	public boolean overlaps(Interval other) {
		return this.start <= other.end && this.end >= other.start;
	}

	// we're assuming that start < end
	public Interval expandToInclude(Interval other) {
		return new Interval(Math.min(start, other.start), Math.max(end, other.end));
	}

	public int center() {
		return (start + end) >>> 1;
	}

	public String toString() {
		return "[" + start + ", " + end + "]";
	}

	/**
	 * Converts a string of the form 1234, 567890 to
	 * a segment or throws a NumberFormatException.
	 * @param coordinates
	 * @return
	 * @throws NumberFormatException
	 */
	public static Interval parse(String coordinates) {
		// split on either a comma, a dash, or whitespace
		String[] coords = coordinates.split("(\\s*,\\s*|\\s*-\\s*|\\s+)");
		if (coords.length > 1) {
			int start = Integer.parseInt(coords[0]);
			int end   = Integer.parseInt(coords[1]);
			return new Interval(start, end);
		}
		else {
			throw new NumberFormatException(coordinates);
		}
	}
}
