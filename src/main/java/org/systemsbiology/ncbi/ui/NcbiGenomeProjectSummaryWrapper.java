package org.systemsbiology.ncbi.ui;

import org.systemsbiology.ncbi.NcbiGenomeProjectSummary;

/**
 * Take an existing NcbiGenomeProjectSummary and wrap it so
 * we can override the toString method. We implement NcbiGenomeProjectSummary
 * to allow equals to work naturally, although it's questionable whether it's
 * worth the trouble.
 */
public class NcbiGenomeProjectSummaryWrapper implements NcbiGenomeProjectSummary {
    private NcbiGenomeProjectSummary summary;

    public NcbiGenomeProjectSummaryWrapper(NcbiGenomeProjectSummary summary) {
        if (summary == null)
            throw new NullPointerException("Can't initialize NcbiGenomeProjectSummaryWrapper with null");
        this.summary = summary;
    }

    public String getGroup() { return summary.getGroup();	}
    public int getNumberOfChromosomes() {	return summary.getNumberOfChromosomes(); }
    public int getNumberOfMitochondria() { return summary.getNumberOfMitochondria(); }
    public int getNumberOfPlasmids() { return summary.getNumberOfPlasmids(); }
    public int getNumberOfPlastids() { return summary.getNumberOfPlastids(); }
    public String getOrganismName() {	return summary.getOrganismName(); }
    public String getProjectId() { return summary.getProjectId(); }
    public String getSuperkingdom() {	return summary.getSuperkingdom();	}
    public String getStatus() {	return summary.getStatus();	}
    public boolean equals(Object other) {
        if (this == other) return true;
        return summary.equals(other);
    }
    public int hashCode() {	return summary.hashCode(); }
    public String toString() { return summary.getOrganismName(); }
    public NcbiGenomeProjectSummary getNcbiGenomeProjectSummary() { return summary;	}
}
