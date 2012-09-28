package org.systemsbiology.ncbi;

import java.util.List;

import org.junit.Test;
import org.systemsbiology.ncbi.EUtilitiesGenomeProjectSummary;
import org.systemsbiology.ncbi.NcbiGenomeProjectSummary;
import org.systemsbiology.ncbi.ProkaryoticGenomeProjectSummary;

import static org.junit.Assert.*;


public class TestNcbiApi {
	
	@Test
	public void testRetrieveProkaryoticGenomeProjects() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<ProkaryoticGenomeProjectSummary> projects = ncbi.retrieveProkaryoticGenomeProjects(ncbi.getNcbiOrganismCode("-- All Archaea --"));

		for (ProkaryoticGenomeProjectSummary project : projects) {
			System.out.println(project);
		}

		assertNotNull(projects);
		assertTrue(projects.size() > 0);
	}

	@Test
	public void testRetrieveIdForRefseq() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		String id = ncbi.retrieveIdForRefseq("NC_001869");
		assertEquals("13234", id);
	}

	@Test
	public void testRetrieveGenomeProjects() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<String> ids = ncbi.retrieveGenomeProjectIds("Halobacterium");
		System.out.println("Halobacterium genome projects: " + ids);
		assertTrue(ids.size() > 0);
	}

	@Test
	public void testRetrieveGenomeProjectsMoose() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<String> ids = ncbi.retrieveGenomeProjectIds("Moose");
		// No moose projects should exist
		System.out.println("Moose genome projects: " + ids);
		assertEquals(0, ids.size());
	}

	@Test
	public void testRetrieveSequencedOrganisms() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<EUtilitiesGenomeProjectSummary> organisms = ncbi.retrieveGenomeProjectSummaries("saccharomyces");
		for (NcbiGenomeProjectSummary no : organisms) {
			System.out.println(no);
		}
		
		assertTrue(organisms.size() > 7);
	}

	@Test
	public void testRetrieveGenomeIds() throws Exception {
		NcbiApi ncbi = new NcbiApi();
		List<String> ids = ncbi.retrieveGenomeIds("217");
		System.out.println("sequences in project 217 = " + ids);
		assertTrue(ids.contains("13234"));
		assertTrue(ids.contains("166"));
		assertTrue(ids.contains("165"));
	}
}
