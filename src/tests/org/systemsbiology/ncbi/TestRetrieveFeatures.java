package org.systemsbiology.ncbi;

import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.ncbi.NcbiSequence;


public class TestRetrieveFeatures {

	@Test
	public void testRetrieveFeatures() throws Exception {
		// may take a while to download the ncbi xml file
		NcbiApi ncbi = new NcbiApi();
		NcbiSequence seq = ncbi.retrieveSequenceAndFeatures("13234");
		
		System.out.println("Found " + seq.getGenes().size() + " features:");
		for (GeneFeatureImpl feature : seq.getGenes()) {
			System.out.println(feature);
		}
	}

	// does this really test anything useful?
	@Test
	public void testError() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		try {
			ncbi.retrieveSequenceAndFeatures("514");
			fail("expected exception...");
		}
		catch (Exception e) {
			System.out.println("Expected error: " + e.getMessage());
			assertTrue(e.getMessage().startsWith("Error code from NCBI: "));
		}
	}

}
