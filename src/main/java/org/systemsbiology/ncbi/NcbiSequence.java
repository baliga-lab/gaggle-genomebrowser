package org.systemsbiology.ncbi;

import java.util.List;

import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;


public class NcbiSequence {
    private String name;
    private long ncbiId;
    private int length;
    private String accession;
    private String locus;
    private String topology;
    private String updateDate;
    private String definition;
    private List<GeneFeatureImpl> genes;

    public NcbiSequence() {}
    public NcbiSequence(String name, int length) {
        this.name = name;
        this.length = length;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLength() { return length;	}
    public void setLength(int length) { this.length = length;	}
    public String getAccession() { return accession; }
    public void setAccession(String accession) { this.accession = accession; }
    public String getLocus() { return locus; }
    public void setLocus(String locus) { this.locus = locus; }
    public long getNcbiId() {	return ncbiId; }
    public void setNcbiId(long ncbiId) { this.ncbiId = ncbiId; }
    public String getTopology() { return topology; }
    public void setTopology(String topology) { this.topology = topology; }
    public String getUpdateDate() {	return updateDate; }
    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }
    public List<GeneFeatureImpl> getGenes() {	return genes;	}
    public void setGenes(List<GeneFeatureImpl> genes) {	this.genes = genes;	}
    public String toString() {
        return "NcbiSequence: [" + name + ", ncbiId=" + ncbiId
            + ", accession=" + accession + ", locus=" + locus + ", length=" + length + "]";
    }
    public void setDefinition(String definition) {
        this.definition = definition;
    }
    public String getDefinition() { return definition; }
}
