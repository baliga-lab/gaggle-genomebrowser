package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;


public class BasicPositionalFeature implements Feature {
	private String seqId;
	private Strand strand;
	private int position;

	public BasicPositionalFeature() {}

	public BasicPositionalFeature(String seqId, Strand strand, int position) {
		this.seqId = seqId;
		this.strand = strand;
		this.position = position;
	}

	public BasicPositionalFeature(Feature feature) {
		this.seqId = feature.getSeqId();
		this.strand = feature.getStrand();
		this.position = feature.getCentralPosition();
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
		return position;
	}
	public int getEnd() {
		return position;
	}
	public int getCentralPosition() {
		return position;
	}
	public String getLabel() {
		return String.format("%s%s:%d", seqId, strand.toAbbreviatedString(), position);
	}
}
