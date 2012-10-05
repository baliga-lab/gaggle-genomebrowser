package org.systemsbiology.ncbi;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.ncbi.NcbiSequence;

public class TestRetrieveSequencesYeast {
    private static final Logger log = Logger.getLogger("unit-test");

    @Test
    public void testRetrieveSequencesForProject() throws Exception {
        NcbiApi ncbi = new NcbiApi();
        List<NcbiSequence> seqs = ncbi.retrieveSequences("128");
        for (NcbiSequence seq : seqs) {
            log.info(seq.getName() + " - " + seq.getAccession());
			
            // the number of "genes" tested here is the number of RNAs plus the number of coding
            // sequences. The number of genes reported by Entrez generally varies from this.
            // These numbers probably change over time as well, so maybe shouldn't be in a unit test.
            
            if ("NC_001133".equals(seq.getAccession())) {
                assertEquals(230208, seq.getLength());
                assertEquals(100, seq.getGenes().size());
            } else if ("NC_001134".equals(seq.getAccession())) {
                assertEquals(813178, seq.getLength());
                assertEquals(423, seq.getGenes().size());
            }	else if ("NC_001135".equals(seq.getAccession())) {
                assertEquals(316617, seq.getLength());
                assertEquals(174, seq.getGenes().size());
            }	else if ("NC_001136".equals(seq.getAccession())) {
                assertEquals(1531918, seq.getLength());
                assertEquals(787, seq.getGenes().size());
            }	else if ("NC_001137".equals(seq.getAccession())) {
                assertEquals(576,869, seq.getLength());
                assertEquals(305, seq.getGenes().size());
            }	else if ("NC_001138".equals(seq.getAccession())) {
                assertEquals(270148, seq.getLength());
                assertEquals(136, seq.getGenes().size());
            }	else if ("NC_001139".equals(seq.getAccession())) {
                assertEquals(1090947, seq.getLength());
                assertEquals(570, seq.getGenes().size());
            }	else if ("NC_001140".equals(seq.getAccession())) {
                assertEquals(562643, seq.getLength());
                assertEquals(296, seq.getGenes().size());
            }	else if ("NC_001141".equals(seq.getAccession())) {
                assertEquals(439885, seq.getLength());
                assertEquals(218, seq.getGenes().size());
            }	else if ("NC_001142".equals(seq.getAccession())) {
                assertEquals(745745, seq.getLength());
                assertEquals(386, seq.getGenes().size());
            }	else if ("NC_001143".equals(seq.getAccession())) {
                assertEquals(666454, seq.getLength());
                assertEquals(333, seq.getGenes().size());
            }	else if ("NC_001144".equals(seq.getAccession())) {
                assertEquals(1078175, seq.getLength());
                assertEquals(562, seq.getGenes().size());
            }	else if ("NC_001145".equals(seq.getAccession())) {
                assertEquals(924429, seq.getLength());
                assertEquals(495, seq.getGenes().size());
            }	else if ("NC_001146".equals(seq.getAccession())) {
                assertEquals(784333, seq.getLength());
                assertEquals(413, seq.getGenes().size());
            }	else if ("NC_001147".equals(seq.getAccession())) {
                assertEquals(1091289, seq.getLength());
                assertEquals(567, seq.getGenes().size());
            }	else if ("NC_001148".equals(seq.getAccession())) {
                assertEquals(948062, seq.getLength());
                // this changed, so we'll fall back to testing with-in a reasonable range
                assertTrue(430 < seq.getGenes().size());
                assertTrue(600 > seq.getGenes().size());
            }	else if ("NC_001224".equals(seq.getAccession())) {
                assertEquals(85779, seq.getLength());
                assertEquals(46, seq.getGenes().size());
            }
        }
    }
}
