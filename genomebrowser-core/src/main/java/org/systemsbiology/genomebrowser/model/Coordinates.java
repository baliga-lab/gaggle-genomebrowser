package org.systemsbiology.genomebrowser.model;

/**
 * Location on the genome. For positional features, use start as position.
 */
public class Coordinates implements Comparable<Coordinates> {
	protected String seqId;
	protected Strand strand;
	protected int start;
	protected int end;

	
	/**
	 * Construct segment coordinates.
	 */
	public Coordinates(String seqId, Strand strand, int start, int end) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
	}

	/**
	 * Construct positional coordinates.
	 */
	public Coordinates(String seqId, Strand strand, int position) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = position;
		this.end = position;
	}

	public String getSeqId() {
		return seqId;
	}
	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}
	public Strand getStrand() {
		return strand;
	}
	public void setStrand(Strand strand) {
		this.strand = strand;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public int getPosition() {
		return this.start;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("(");
		sb.append(seqId).append(", ");
		sb.append(strand.toAbbreviatedString()).append(", ");
		sb.append(start).append(", ");
		sb.append(end).append(")");
		return sb.toString();
	}

	@Override
	public int compareTo(Coordinates other) {
		if (other==null) return -1;
		int result = this.seqId.compareTo(other.getSeqId());
		if (result==0) {
			result = this.strand.compareTo(other.strand);
			if (result==0) {
				result = this.start - other.start;
				if (result==0) {
					result = this.end - other.end;
				}
			}
		}
		return result;
	}
	
}
