package org.systemsbiology.genomebrowser.impl;

import org.systemsbiology.genomebrowser.model.FeatureFields;

public class QuantitativePositionalFeatureFields implements FeatureFields {
	public String sequence;
	public String strand;
	public int position;
	public double value;

	public String getSequenceName() { return sequence; }
	public String getStrand() {	return strand; }
	public int getStart() {	return position; }
	public int getEnd() { return position; }
	public int getPosition() { return position; }
	public double getValue() { return value; }
	public String getName() {	return null; }
	public String getCommonName() {	return null; }
	public String getGeneType() {	return null; }

	public void set(String sequence, String strand, int position, double value) {
		this.sequence = sequence;
		this.strand = strand;
		this.position = position;
		this.value = value;
	}
}