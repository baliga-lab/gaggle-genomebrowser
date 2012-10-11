package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.FeatureFields;

public class QuantitativeSegmentFeatureFields implements FeatureFields {
	public String sequence;
	public String strand;
	public int start;
	public int end;
	public double value;

	public String getSequenceName() { return sequence; }
	public String getStrand() {	return strand; }
	public int getStart() {	return start; }
	public int getEnd() {	return end; }
	public int getPosition() { return start; }
	public double getValue() { return value; }
	public String getName() { return null; }
	public String getCommonName() {	return null; }
	public String getGeneType() {	return null; }

	public void set(String seqId, String strand, int start, int end, double value) {
		this.sequence = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
		this.value = value;
	}
}