package org.systemsbiology.ncbi;

/**
 * An object that describes an NCBI genome project. Implementations should
 * implement equals and hashcode (see NcbiGenomeProjectSummaryWrapper).
 */
public interface NcbiGenomeProjectSummary {
	public String getProjectId();
	public String getOrganismName();
	public String getSuperkingdom();
	public String getGroup();
	public String getStatus();
	public int getNumberOfChromosomes();
	public int getNumberOfPlasmids();
	public int getNumberOfMitochondria();
	public int getNumberOfPlastids();
}