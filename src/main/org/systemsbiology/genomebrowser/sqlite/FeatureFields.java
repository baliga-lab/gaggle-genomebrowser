/**
 * 
 */
package org.systemsbiology.genomebrowser.sqlite;

// TODO should it be FeatureFields responsibility to validate Strand and GeneType values?
// Those values must come from a restricted set of strings.

/**
 * A feature is a position on the genome augmented by some arbitrary additional
 * fields. This class is an attempt to accommodate all known cases as a superset
 * of all fields currently in use by a quantitative segment, quantitative positional,
 * or gene feature. We also allow sequence to be specified as as arbitrary string
 * that will be mapped to a standard sequence ID later.
 * 
 * Is this adequate for extensibility to unknown feature types?
 * 
 * They may have other fields (error bars or p-values for example). That might be
 * covered by subclassing FeatureFields. Other cases might encode some or all fields
 * in a String (chr+:1000-1200, for example). These cases are handled already (in 
 * DataMatrixFeatureSource, for example) by doing the conversion first in the
 * FeatureSource implementation, then calling featureProcessor.process(fields).
 * 
 * Maybe something more flexible? Start with a truly arbitrary
 * set of fields, accompanied by a schema and a means of transforming that schema
 * to a standard form known to the program. Carrying the schema with each object
 * would be inefficient. Maybe a collection of similar FeatureFeilds object with a
 * schema attached to the collection?
 */
public interface FeatureFields {
	public String getSequenceName();
	public String getStrand();
	public int getStart();
	public int getEnd();
	public int getPosition();
	public double getValue();
	public String getName();
	public String getCommonName();
	public String getGeneType();
}
