package org.systemsbiology.ncbi;

/**
 * Fits the data returned by the lproks.cgi script.
 * @author cbare
 */
public class ProkaryoticGenomeProjectSummary extends EUtilitiesGenomeProjectSummary {
    private int taxId;
    private float genomeSize;
    private Float gcContent;
    private String releasedDate;
    private String[] accessions;

    public ProkaryoticGenomeProjectSummary() {}
    public int getTaxId() { return taxId;	}
    public void setTaxId(int taxId) { this.taxId = taxId; }
    public float getGenomeSize() { return genomeSize; }
    public void setGenomeSize(float genomeSize) {	this.genomeSize = genomeSize;	}
    public Float getGcContent() {	return gcContent;	}
    public void setGcContent(Float gcContent) {	this.gcContent = gcContent;	}
    public String getReleasedDate() { return releasedDate; }
    public void setReleasedDate(String releasedDate) {
        this.releasedDate = releasedDate;
    }
    public String[] getAccessions() { return accessions; }
    public void setAccessions(String[] accessions) {
        this.accessions = accessions;
    }

    public String toString() {
        return "GenomeProject {" + getProjectId() + ", " + getTaxId() + ", " +
            getOrganismName() + ", " +
            getSuperkingdom() + ", " + getGroup() + ", " +
            getGenomeSize() + ", " + getGcContent() + ", " +
            getNumberOfChromosomes() + ", " + getNumberOfPlasmids() + ", " +
            getReleasedDate() + ", " +
            join(",", accessions) + "}";
    }

    private String join(String delimiter, String[] strings) {
        if (strings == null || strings.length==0) return "";
        StringBuilder sb = new StringBuilder(strings[0]);
        for (int i=1; i<strings.length; i++) {
            sb.append(delimiter).append(strings[i]);
        }
        return sb.toString();
    }
}