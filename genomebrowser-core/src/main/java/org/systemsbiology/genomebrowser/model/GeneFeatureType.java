package org.systemsbiology.genomebrowser.model;


/**
 * Loosely based on NCBI feature keys
 * @author cbare
 */
public enum GeneFeatureType {
	gene, cds, trna, rrna, rna, ncrna, repeat, operon, pfam, peptide, other;

	/**
	 * converts a string to a GeneFeatureType, with "gene" as a default if
	 * no matching value is found.
	 */
	public static GeneFeatureType fromString(String value) {
		return fromString(value, gene);
	}

	public static GeneFeatureType fromString(String value, GeneFeatureType defaultValue) {
		for (GeneFeatureType type: values()) {
			if (type.toString().equalsIgnoreCase(value))
				return type;
		}
		return defaultValue;
	}
	
}