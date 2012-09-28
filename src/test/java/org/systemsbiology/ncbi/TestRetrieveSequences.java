package org.systemsbiology.ncbi;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.ncbi.NcbiSequence;

import static org.junit.Assert.*;


public class TestRetrieveSequences {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void testRetrieveSequencesForProject() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<NcbiSequence> seqs = ncbi.retrieveSequences("217");
		for (NcbiSequence seq : seqs) {
			log.info(seq.getName() + " - " + seq.getAccession());
			if ("NC_002607".equals(seq.getAccession())) {
				assertEquals(2014239, seq.getLength());
				assertEquals(2127, seq.getGenes().size());
			}
			else if ("NC_002608".equals(seq.getAccession())) {
				assertEquals(365425, seq.getLength());
				assertEquals(371, seq.getGenes().size());
			}
			else if ("NC_001869".equals(seq.getAccession())) {
				assertEquals(191346, seq.getLength());
				assertEquals(176, seq.getGenes().size());
			}
		}
	}
}
