package org.systemsbiology.genomebrowser.bookmarks;

import java.util.Arrays;

import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.genomebrowser.util.Attributes$;

/**
 * A Bookmark is a specialization of Feature, which can have a text annotation
 * attached. Bookmarks may also have associated features. The associated features
 * are identified by names, since we don't have an enforced unique identifier for
 * features in general. Instances of NamedFeature have a name
 */
public class Bookmark implements Feature {
	private String seqId;
	private Strand strand;
	private int start;
	private int end;
	private String label;
	private String annotation;	
	private String sequence; //new var for seq value (dmartinez)
	private String[] associatedFeatureName;

	// attributes field of the form "key1=value1;key2=value2;"
	// to be used by specialized bookmarks (ex: transcript boundaries)
	private String attributes;	

	public Bookmark() { }

	/**
	 * Create a bookmark from the given feature. If the feature is a named
	 * feature, we associate that feature with the bookmark by recording its
	 * name.
	 */
	public Bookmark(Feature feature) {
		if (feature != null) {
			this.seqId = feature.getSeqId();
			this.strand = feature.getStrand();
			this.start = feature.getStart();
			this.end = feature.getEnd();
			this.label = feature.getLabel();
			if (feature instanceof NamedFeature)
				this.setAssociatedFeatureNames(((NamedFeature)feature).getName());
			else if (feature instanceof Bookmark) {
				this.annotation = ((Bookmark)feature).annotation;
				this.sequence = ((Bookmark)feature).sequence; // copy as annotation (dmartinez)
				this.setAssociatedFeatureNames(((Bookmark)feature).getAssociatedFeatureNames());
				this.attributes = ((Bookmark)feature).attributes;
			}
		}
	}

	public Bookmark(String seqId, int start, int end) {
      this(seqId, Strand.none, start, end);
	}

	public Bookmark(String seqId, Strand strand, int start, int end) {
      this(seqId, strand, start, end, null, null, null, null);
	}

	public Bookmark(String seqId, Strand strand, int start, int end, String label,
                  String annotation) {
      this(seqId, strand, start, end, label, annotation, null, null);
	}

	public Bookmark(String seqId, Strand strand, int start, int end, String label,
                  String annotation, String attributes) {
      this(seqId, strand, start, end, label, annotation, attributes, null);
	}

	// new Bookmark-one with sequence (dmartinez)
	public Bookmark(String seqId, Strand strand, int start, int end, String label,
                  String annotation, String attributes, String sequence) {
		this.seqId = seqId;
		this.strand = strand;
		this.start = start;
		this.end = end;
		this.label = label;
		this.annotation = annotation;
		this.attributes = attributes;
		this.sequence = sequence;
	}

	public int getStart() {	return start;	}
	public void setStart(int start) {	this.start = start; }
	public int getEnd() {	return end; }
	public void setEnd(int end) {	this.end = end;	}
	public int getCentralPosition() {
		// overflow-safe integer average
		return (start + end) >>> 1;
	}
	public String getAnnotation() { return annotation == null ? "" : annotation; }
	public void setAnnotation(String annotation) { this.annotation = annotation; }
	
	// getter 'n setter of sequence var (dmartinez)
	public String getSequence() {	return sequence == null ? "" : sequence; }
	public void setSequence(String sequence) { this.sequence = sequence; }
	public Strand getStrand() { return strand == null ? Strand.none : strand; }
	public void setStrand(Strand strand) { this.strand = strand; }
	public String getSeqId() { return seqId; }
	public void setSeqId(String seqId) { this.seqId = seqId; }
	public String getLabel() {
		return label == null ? "[" + getStart() + ", " + getEnd() + "]" : label;
	}
	public void setLabel(String label) { this.label = label; }
	public String getToolTip() { return annotation; }
	public void setAssociatedFeatureNames(String... associatedFeatureName) {
		this.associatedFeatureName = associatedFeatureName;
	}

	/**
	 * Associated features are the names of features associated with this
	 * bookmark.
	 */
	public String[] getAssociatedFeatureNames() {
		return associatedFeatureName;
	}
	public Attributes getAttributes() {
		return Attributes$.MODULE$.parse(attributes);
	}
	public String getAttributesString() {	return attributes; }
	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}
	public boolean hasAttributes() { return this.attributes != null; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotation == null) ? 0 : annotation.hashCode());
        result = prime * result + Arrays.hashCode(associatedFeatureName);
        result = prime * result + end;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((seqId == null) ? 0 : seqId.hashCode());
        result = prime * result + start;
        result = prime * result + ((strand == null) ? 0 : strand.hashCode());
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Bookmark other = (Bookmark) obj;
        if (annotation == null) {
            if (other.annotation != null)	return false;
        }	else if (!annotation.equals(other.annotation)) return false;
        if (!Arrays.equals(associatedFeatureName, other.associatedFeatureName))	return false;
        if (end != other.end)	return false;
        if (label == null) {
            if (other.label != null) return false;
        }	else if (!label.equals(other.label)) return false;
        if (seqId == null) {
            if (other.seqId != null) return false;
        }	else if (!seqId.equals(other.seqId)) return false;
        if (start != other.start)	return false;
        if (strand == null) {
            if (other.strand != null)	return false;
        }	else if (!strand.equals(other.strand)) return false;
        if (attributes == null) {
            if (other.attributes != null)	return false;
        }	else if (!attributes.equals(other.attributes)) return false;
        return true;
    }

    public String toString() {
        return label != null ? label :
            String.format("%s%s:%d-%d", seqId, strand.toAbbreviatedString(), start, end);
    }
}
