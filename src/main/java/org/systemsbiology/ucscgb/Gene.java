package org.systemsbiology.ucscgb;


/**
 * A class to temporarily hold the fields of a UCSC gene before we convert
 * the data to another format.
 *
 * Note:
 * UCSC uses a C-style zero based indexing for genome coordinates. Don't
 * forget to correct start positions.
 *
 * @author cbare
 */
public class Gene {
    public final String name;
    public final String chrom;
    public final String strand;
    public final int txStart;
    public final int txEnd;
    public final int cdsStart;
    public final int cdsEnd;
    public final int exonCount;
    public final String exonStarts;
    public final String exonEnds;
    public final String id;
    public final String name2;
    public final String cdsStartStat;
    public final String cdsEndStat;
    public final String exonFrames;
	
    // @see org.systemsbiology.genomebrowser.model.GeneFeatureType
    // I didn't want to introduce a dependency on the model here, so I did
    // something even worse and introduced an implicit dependency. The value
    // of geneType should match a value of GeneFeature.FeatureType enum.
    // public enum FeatureType {gene, cds, trna, rrna, rna, ncrna, repeat, other};

    // If geneType is an unknown value, it will be converted to "gene" when we
    // convert to a GeneFeature in
    // org.systemsbiology.genomebrowser.ucscgb.ImportUcscGenome
    public final String geneType;


    public String toString() {
        return String.format("(Gene: name=%s, name2=%s, type=%s, chr=%s, %s, coords=(%,d-%,d))",
                             name, name2, geneType, chrom, strand, cdsStart, cdsEnd);
    }

    public Gene(String name, String chrom, String strand,
                int txStart, int txEnd,
                int cdsStart, int cdsEnd,
                int exonCount, String exonStarts, String exonEnds,
                String id, String name2,
                String cdsStartStat, String cdsEndStat, String exonFrames,
                String type) {
        this.name = name;
        this.chrom = chrom;
        this.strand = strand;
        this.cdsStart = cdsStart;
        this.cdsEnd = cdsEnd;
        this.cdsStartStat = cdsStartStat;
        this.cdsEndStat = cdsEndStat;
        this.exonCount = exonCount;
        this.exonStarts = exonStarts;
        this.exonEnds = exonEnds;
        this.exonFrames = exonFrames;
        this.id = id;
        this.name2 = name2;
        this.txStart = txStart;
        this.txEnd = txEnd;
        this.geneType = type;
    }

    /**
     * constructor for RNA genes
     */
    public Gene(String name, String chrom, String strand,
                int txStart, int txEnd,
                int cdsStart, int cdsEnd,
                String id, String name2, String type) {
        this.name = name;
        this.chrom = chrom;
        this.strand = strand;
        this.txStart = txStart;
        this.txEnd = txEnd;
        this.cdsStart = cdsStart;
        this.cdsEnd = cdsEnd;
        this.exonCount = 1;
        this.exonStarts = null;
        this.exonEnds = null;
        this.id = id;
        this.name2 = name2;
        this.cdsStartStat = null;
        this.cdsEndStat = null;
        this.exonFrames = null;
        this.geneType = type;
    }
}
