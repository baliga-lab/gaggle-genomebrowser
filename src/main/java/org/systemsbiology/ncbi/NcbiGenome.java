package org.systemsbiology.ncbi;

import java.util.List;

public interface NcbiGenome extends NcbiGenomeProjectSummary {
    List<NcbiSequence> getSequences();
}
