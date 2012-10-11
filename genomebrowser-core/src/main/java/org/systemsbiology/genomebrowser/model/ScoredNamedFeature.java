package org.systemsbiology.genomebrowser.model;


// TODO remove?
public class ScoredNamedFeature extends GeneFeatureImpl implements Feature.ScoredFeature {
	private double score;

	public ScoredNamedFeature(String seqId, Strand strand, int start, int end, String name, GeneFeatureType type, double score) {
		super(seqId, strand, start, end, name, type);
		this.score = score;
	}

	public ScoredNamedFeature(String seqId, Strand strand, int start, int end, String name, String commonName, GeneFeatureType type, double score) {
		super(seqId, strand, start, end, name, commonName, type);
		this.score = score;
	}

	@Override
	public double getScore() {
		return score;
	}

}
