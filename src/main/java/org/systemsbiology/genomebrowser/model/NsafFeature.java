package org.systemsbiology.genomebrowser.model;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import org.systemsbiology.util.Selectable;

public class NsafFeature implements GeneFeature, Selectable {

	/*
	    CREATE TABLE features_nsaf (
		  sequences_id integer, strand text, start integer, end integer, gene_type text,
		  NSAF_in_Media_Secretion2_April_24_2009 numeric,
		  NSAF_in_SEC_Secretion1_April_16_2009 numeric,
		  NSAF_in_SEC_Secretion2_April_24_2009 numeric,
		  NSAF_in_SEC_Secretion2_Mar_10_2009 numeric,
		  NSAF_in_SEC_Secretome_July_13_2009 numeric,
		  NSAF_in_s_MEM_SLayer_July_9_2009 numeric,
		  name text,
		  common_name text,
		  degenerate integer
		);

	 */

	private String seqId;
	private int start;
	private int end;
	private Strand strand;
	private String name;
	private String commonName;
	private boolean selected;
	private GeneFeatureType type;

	private float[] nsaf;
	private boolean degenerate;
	
	public NsafFeature(String seqId, Strand strand, int start, int end, String name, String commonName, GeneFeatureType type, float[] nsaf, boolean degenerate) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = Math.min(start, end);
		this.end = Math.max(start, end);
		this.name = name;
		this.commonName = commonName;
		this.type = type==null ? GeneFeatureType.gene : type;
		this.nsaf = nsaf;
		this.degenerate = degenerate;
	}

	public boolean isDegenerate() {
		return degenerate;
	}

	public float[] getNsaf() {
		return nsaf;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
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

	public String getName() {
		return name;
	}

	public String getCommonName() {
		return commonName;
	}

	public String getLabel() {
		return isNullOrEmpty(commonName) ? name : commonName;
	}

	public String getNameAndCommonName() {
		if (commonName == null || commonName.equals(name))
			return name;
		else
			return name + " " + commonName;
	}

	public boolean selected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public GeneFeatureType getType() {
		return type;
	}

	public String toString() {
		return "nsaf " + name + ((commonName==null) ? "" : "(" + commonName + ")");
	}
}
