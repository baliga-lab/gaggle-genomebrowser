package org.systemsbiology.ncbi;


import java.util.List;

import static org.junit.Assert.*;
import org.apache.log4j.Logger;
import org.junit.Test;



/**
 * Tests handling of an incomplete genome project.
 * At some point, the project will be complete
 * and this test will no longer be valid.
 */
@SuppressWarnings("unused")
public class TestEColi {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void testRetrieveGenomeProjectsEColi() throws Exception {
		NcbiApi gp = new NcbiApi();
		List<String> ids = gp.retrieveGenomeProjectIds("Escherichia coli LW1655F+");
		log.info("project ids = " + ids);
	}

	@Test
	public void testRetrieveGenomeIds() throws Exception {
		NcbiApi gp = new NcbiApi();
		List<String> ids = gp.retrieveGenomeIds("29609");
		log.info("sequences in project 29609 = " + ids);
	}

	@Test
	public void testRetrieveProjectSummary() throws Exception {
		NcbiApi gp = new NcbiApi();
		EUtilitiesGenomeProjectSummary summary = gp.retrieveGenomeProjectSummary("29609");
		log.info("summary = " + summary);

		// the status is "inprogress" at this point. Assert is really not needed. The
		// test is just to prove we don't blow up if there are no sequences.
		//assertEquals("inprogress", summary.getStatus());
	}

}
