package org.systemsbiology.ucscgb;

import java.util.Collection;
import java.util.Comparator;

/**
 * Represents a genome in the UCSC genome browser. See the dbDb and clade
 * tables in the database hgcentral in the UCSC genome browser's public
 * MySQL instance.
 */
public class Genome {
    private String dbName;
    private String scientificName;
    private String genome;
    private String description;
    private long taxid;
    private String clade;
    private String domain;
    private String geneTable;

    public static final Comparator<Genome> comparator = new Comparator<Genome>() {
        public int compare(Genome g1, Genome g2) {
            return g1.getScientificName().compareTo(g2.getScientificName());
        }
    };

    /**
     * Linear search by scientificName through a collection of Genomes.
     * @return first genome with matching scientific name or null if none found.
     */
    public static Genome findByScientificName(Collection<Genome> genomes, String scientificName) {
        for (Genome genome : genomes) {
            if (scientificName.equals(genome.scientificName)) {
                return genome;
            }
        }
        return null;
    }

    public Genome(String dbName, String scientificName, String domain, String clade) {
        this.dbName = dbName;
        this.scientificName = scientificName;
        this.domain = domain;
        this.clade = clade;
    }

    public Genome(String dbName, String description, String genome,
                  String scientificName, long taxid, String clade, String domain) {
        this.dbName = dbName;
        this.description = description;
        this.genome = genome;
        this.scientificName = scientificName;
        this.taxid = taxid;
        this.clade = clade;
        this.domain = domain;
    }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }
    public String getGenome() {	return genome; }
    public void setGenome(String genome) { this.genome = genome; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getTaxid() { return taxid;	}
    public void setTaxid(long taxid) { this.taxid = taxid; }
    public String getClade() { return clade; }
    public void setClade(String clade) { this.clade = clade; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public Category getCategory() {	return Category.fromDomainAndClade(domain, clade); }
    public String getGeneTable() { return geneTable; }
    public void setGeneTable(String geneTable) { this.geneTable = geneTable; }
    public String toString() { return scientificName;	}

    public String toDebugString() {
        StringBuilder sb = new StringBuilder("(");
        sb.append(dbName);
        sb.append(", ").append(description);
        sb.append(", ").append(genome);
        sb.append(", ").append(scientificName);
        sb.append(", ").append(domain);
        sb.append(", ").append(clade);
        sb.append(", ").append(taxid);
        sb.append(")");
        return sb.toString();
    }
}
