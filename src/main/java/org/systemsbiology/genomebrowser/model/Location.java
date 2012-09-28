package org.systemsbiology.genomebrowser.model;

// TODO redundant with coordinates, segment and interval WTF?

/**
 * A location on the genome
 */
public class Location {
	public final Sequence sequence;
	public final Strand strand;
	public final int start;
	public final int end;

	public Location(Sequence sequence, Strand strand, int start, int end) {
		this.sequence = sequence;
		this.strand = strand;
		this.start = start;
		this.end = end;
	}

	public String toString() {
		return "[\"" + sequence.getSeqId() + strand.toAbbreviatedString() + "\", " + start + ", " + end + "]";
	}
}
