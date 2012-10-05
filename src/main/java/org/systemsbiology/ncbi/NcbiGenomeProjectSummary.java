package org.systemsbiology.ncbi;

/**
 * An object that describes an NCBI genome project. Implementations should
 * implement equals and hashcode (see NcbiGenomeProjectSummaryWrapper).
 */
public interface NcbiGenomeProjectSummary {
    String getProjectId();
    String getOrganismName();
    String getSuperkingdom();
    String getGroup();
    String getStatus();
    int getNumberOfChromosomes();
    int getNumberOfPlasmids();
    int getNumberOfMitochondria();
    int getNumberOfPlastids();
}
