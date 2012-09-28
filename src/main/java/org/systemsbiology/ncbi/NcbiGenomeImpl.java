/**
 * 
 */
package org.systemsbiology.ncbi;

import java.util.List;


class NcbiGenomeImpl implements NcbiGenome {
	private final NcbiGenomeProjectSummary summary;
	private final List<NcbiSequence> sequences;

	NcbiGenomeImpl(NcbiGenomeProjectSummary summary, List<NcbiSequence> sequences) {
		this.summary = summary;
		this.sequences = sequences;
	}

	public List<NcbiSequence> getSequences() {
		return sequences;
	}

	public String getGroup() {
		return summary.getGroup();
	}

	public int getNumberOfChromosomes() {
		return summary.getNumberOfChromosomes();
	}

	public int getNumberOfMitochondria() {
		return summary.getNumberOfMitochondria();
	}

	public int getNumberOfPlasmids() {
		return summary.getNumberOfPlasmids();
	}

	public int getNumberOfPlastids() {
		return summary.getNumberOfPlastids();
	}

	public String getOrganismName() {
		return summary.getOrganismName();
	}

	public String getProjectId() {
		return summary.getProjectId();
	}

	public String getSuperkingdom() {
		return summary.getSuperkingdom();
	}

	public String getStatus() {
		return summary.getStatus();
	}

	@Override
	public int hashCode() {
		return summary.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		return summary.equals(obj);
	}
}