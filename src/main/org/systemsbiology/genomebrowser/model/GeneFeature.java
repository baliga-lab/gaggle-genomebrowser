package org.systemsbiology.genomebrowser.model;

import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;


public interface GeneFeature extends NamedFeature {
	public String getCommonName();
	public GeneFeatureType getType();
}
