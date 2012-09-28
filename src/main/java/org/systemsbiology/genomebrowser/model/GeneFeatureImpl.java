package org.systemsbiology.genomebrowser.model;

import static org.systemsbiology.util.StringUtils.isNullOrEmpty;

import org.systemsbiology.util.Selectable;


/**
 * A feature that is a named region of biological interest on a sequence.
 * Genes are not necessarily protein coding, but (here anyway) include t-rna,
 * r-rna, nc-rna, and other features.
 * 
 * The selected property of GeneFeature depends on the objects being kept in
 * memory as they are with the list-based implementation of GeneTrack.
 *
 * @author cbare
 */
public class GeneFeatureImpl implements GeneFeature, Selectable {
	private String seqId;
	private int start;
	private int end;
	private Strand strand;
	private String name;
	private String commonName;
	private boolean selected;
	private GeneFeatureType type;



	public GeneFeatureImpl(String seqId, Strand strand, int start, int end, String name, GeneFeatureType type) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = Math.min(start, end);
		this.end = Math.max(start, end);
		this.name = name;
		this.type = type==null ? GeneFeatureType.gene : type;
	}

	public GeneFeatureImpl(String seqId, Strand strand, int start, int end, String name, String commonName, GeneFeatureType type) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = Math.min(start, end);
		this.end = Math.max(start, end);
		this.name = name;
		this.commonName = commonName;
//        this.name = commonName; // easier to browse if: gene name is VNGxxxx and hint is common name plus some annotation
//        this.commonName = name; // by rvencio & dmartinez Jan/2012.
		// Maybe a better way to implement this would be to add an optional annotation field? -jcb
		// Also see: TypedGeneTrackRenderer.useCommonNames. This causes the genes to be labeled
		// with commonName, if true and with name if false. It's true by default, but can be
		// changed in the track's properties.
		this.type = type==null ? GeneFeatureType.gene : type;
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
		return name + ((commonName==null) ? "" : "(" + commonName + ")");
	}
}
