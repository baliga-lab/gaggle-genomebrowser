package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * A simple implementation of a quantitative feature. In practice,
 * quantitative features will usually be implemented using Flyweights
 * backed by primitive arrays. This implementation exists for small
 * quantities of data and for testing.
 * 
 * @author cbare
 */
public class BasicQuantitativeFeature implements Feature.Quantitative {
	protected String seqId;
	protected Strand strand;
	protected int start;
	protected int end;
	protected double value;

	public BasicQuantitativeFeature(String seqId, Strand strand, int start, int end, double value) {
		super();
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
		this.value = value;
	}

	public String getSeqId() {
		return seqId;
	}

	public Strand getStrand() {
		return strand;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getCentralPosition() {
		// average without overflow
		return (start + end) >>> 1;
	}

	public String getLabel() {
		return String.format("%.2f", value);
	}

	public double getValue() {
		return value;
	}

	public String toString() {
		return "Feature(" + seqId + ", " + strand.toAbbreviatedString() + ", " + start + ", " + end
		+ ", " + value + ")";
	}
}
