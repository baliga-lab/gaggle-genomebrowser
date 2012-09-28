package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.util.MathUtils;


public class BasicFeature implements Feature {
	private String seqId;
	private Strand strand;
	private int start;
	private int end;
	private String label;

	

	public BasicFeature(String seqId, Strand strand, int start, int end) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
	}

	public BasicFeature(String seqId, Strand strand, int start, int end, String label) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
		this.label = label;
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
	public String getLabel() {
		if (label==null)
			return String.format("%s%s:%d-%d", seqId, strand.toAbbreviatedString(), start, end);
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public int getCentralPosition() {
		return MathUtils.average(start, end);
	}
}
