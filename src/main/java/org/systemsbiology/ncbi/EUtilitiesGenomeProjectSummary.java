package org.systemsbiology.ncbi;

/**
 * hold the results of EUtilities esummary method for a genome project. 
 */
public class EUtilitiesGenomeProjectSummary implements NcbiGenomeProjectSummary {

    private String projectId;
    private String organismName;
    private String superkingdom;
    private String group;
    private int numberOfChromosomes;
    private int numberOfPlasmids;
    private int numberOfMitochondria;
    private int numberOfPlastids;
    private String status;

    public EUtilitiesGenomeProjectSummary() {}

    public EUtilitiesGenomeProjectSummary(String id, String name, String kingdom, String group,
                                          int numberOfChromosomes, int numberOfPlasmids,
                                          int numberOfMitochondria, int numberOfPlastids,
                                          String status) {
        this.projectId = id;
        this.organismName = name;
        this.superkingdom = kingdom;
        this.group = group;
        this.numberOfChromosomes = numberOfChromosomes;
        this.numberOfPlasmids = numberOfPlasmids;
        this.numberOfMitochondria = numberOfMitochondria;
        this.numberOfPlastids = numberOfPlastids;
        this.status = status;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) {
      this.projectId = projectId;
    }
    public String getOrganismName() {	return organismName; }
    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }
    public String getSuperkingdom() {	return superkingdom; }
    public void setSuperKingdom(String superKingdom) {
        this.superkingdom = superKingdom;
    }
    public String getGroup() {return group;	}
    public void setGroup(String group) {
        this.group = group;
    }
    public int getNumberOfChromosomes() { return numberOfChromosomes;	}
    public void setNumberOfChromosomes(int numberOfChromosomes) {
        this.numberOfChromosomes = numberOfChromosomes;
    }
    public int getNumberOfPlasmids() { return numberOfPlasmids; }
    public void setNumberOfPlasmids(int numberOfPlasmids) {
        this.numberOfPlasmids = numberOfPlasmids;
    }
    public int getNumberOfMitochondria() { return numberOfMitochondria; }
    public int getNumberOfPlastids() { return numberOfPlastids; }
    public String getStatus() {	return status; }
    public String toString() {
        return String.format("[GenomeProject: id=%s organism=%s, superkingdom=%s, group=%s, status=%s, " +
                             "chrs=%d, mitoch=%d, pasmids=%d, plastids=%d]",
                             projectId, organismName, superkingdom, group, 
                             status, numberOfChromosomes, numberOfMitochondria, numberOfPlasmids, numberOfPlastids);
    }

    /**
     * Two instances of NcbiGenomeProjectSummary are equal if they have the same projectId.
     */
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof NcbiGenomeProjectSummary)
            return this.getProjectId() == ((NcbiGenomeProjectSummary)other).getProjectId();
        else return false;
    }

    public int hashCode() { return this.projectId.hashCode(); }
}
