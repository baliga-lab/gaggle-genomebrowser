package org.systemsbiology.genomebrowser.sqlite;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;


public class TestHeuristicSequenceMapper {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void testBasic() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chromosome", 1);
		map.put("pNRC200", 2);
		map.put("pNRC100", 3);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("chromosome"));
		assertEquals(2, hsm.getId("pNRC200"));
		assertEquals(3, hsm.getId("pNRC100"));

		assertEquals(1, hsm.getId("chr"));
		assertEquals(1, hsm.getId("Chromosome"));
		assertEquals(1, hsm.getId("CHR"));

		assertEquals(2, hsm.getId("pnrc200"));
		assertEquals(2, hsm.getId("PNRC200"));
	}

	@Test
	public void testChr() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chr", 1);
		map.put("pNRC200", 2);
		map.put("pNRC100", 3);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("chr"));
		assertEquals(1, hsm.getId("chromosome"));
		assertEquals(1, hsm.getId("chromosome-1"));
		assertEquals(1, hsm.getId("chr1"));
		assertEquals(1, hsm.getId("CHR1"));
		assertEquals(1, hsm.getId("CHR"));
		assertEquals(1, hsm.getId("Chromosome"));
	}

	@Test
	public void testPlasmid() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chromosome", 1);
		map.put("pNRC200", 2);
		map.put("pNRC100", 3);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(2, hsm.getId("plasmid pNRC200"));
		assertEquals(2, hsm.getId("plasmid_pNRC200"));

		map = new HashMap<String, Integer>();
		map.put("chromosome", 1);
		map.put("plasmid pNRC200", 2);
		map.put("plasmid pNRC100", 3);

		hsm = new HeuristicSequenceMapper(map);

		assertEquals(2, hsm.getId("plasmid pNRC200"));
		assertEquals(2, hsm.getId("plasmid_pNRC200"));
		assertEquals(2, hsm.getId("pNRC200"));
		assertEquals(2, hsm.getId("pnrc200"));
		assertEquals(2, hsm.getId("PNRC200"));
	}

	@Test
	public void testNumberedChromosomes() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chr1", 1);
		map.put("chr2", 2);
		map.put("chr3", 3);
		map.put("chr4", 4);
		map.put("chr5", 5);
		map.put("chr6", 6);
		map.put("chr7", 7);
		map.put("chr8", 8);
		map.put("chr9", 9);
		map.put("chr10", 10);
		map.put("chr11", 11);
		map.put("chr12", 12);
		map.put("chr13", 13);
		map.put("chr14", 14);
		map.put("chr15", 15);
		map.put("chr16", 16);
		map.put("chrM", 17);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("chr1"));
		assertEquals(1, hsm.getId("chr-1"));
		assertEquals(1, hsm.getId("chr 1"));
		assertEquals(1, hsm.getId("chr_1"));
		assertEquals(1, hsm.getId("chromosome1"));
		assertEquals(1, hsm.getId("chromosome-1"));
		assertEquals(1, hsm.getId("chromosome 1"));
		assertEquals(1, hsm.getId("chromosome_1"));

		assertEquals(5, hsm.getId("chr5"));
		assertEquals(6, hsm.getId("chr-6"));
		assertEquals(7, hsm.getId("chr 7"));
		assertEquals(8, hsm.getId("chr_8"));
		assertEquals(9, hsm.getId("chromosome9"));
		assertEquals(10, hsm.getId("chromosome-10"));
		assertEquals(11, hsm.getId("chromosome 11"));
		assertEquals(12, hsm.getId("chromosome_12"));

		assertEquals(1, hsm.getId("1"));
		assertEquals(2, hsm.getId("2"));
		assertEquals(3, hsm.getId("3"));
		assertEquals(5, hsm.getId("5"));
		assertEquals(7, hsm.getId("7"));
		assertEquals(11, hsm.getId("11"));
		assertEquals(13, hsm.getId("13"));

		assertEquals(17, hsm.getId("chrM"));
	}

	@Test
	public void testChromosomeM() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chr1", 1);
		map.put("chr2", 2);
		map.put("chr3", 3);
		map.put("chrM", 17);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(17, hsm.getId("chromosome_M"));
		assertEquals(17, hsm.getId("chromosome m"));
	}

	@Test
	public void testChromosomeNotEqualsPlasmid() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chr1", 1);
		map.put("chr2", 2);
		map.put("chr3", 3);
		map.put("plasmid 1", 4);
		map.put("plasmid 2", 5);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("1"));
		assertEquals(2, hsm.getId("2"));
		assertEquals(3, hsm.getId("3"));
		assertEquals(4, hsm.getId("plasmid 1"));
		assertEquals(4, hsm.getId("Plasmid_1"));
		assertEquals(4, hsm.getId("plasmid_1"));
		assertEquals(4, hsm.getId("plasmid-1"));
		assertEquals(5, hsm.getId("plasmid 2"));
	}

	@Test
	public void testLowerCaseKeys() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("ASDF", 1);
		map.put("asdf", 2);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("ASDF"));
		assertEquals(2, hsm.getId("asdf"));
	}

	@Test
	public void testABC() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chromosome A", 1);
		map.put("chromosome B", 2);
		map.put("chromosome C", 3);
		map.put("chromosome D", 4);
		map.put("plasmid A", 5);
		map.put("plasmid B", 6);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("A"));
		assertEquals(2, hsm.getId("B"));
		assertEquals(3, hsm.getId("C"));
		assertEquals(5, hsm.getId("plasmid A"));
		assertEquals(6, hsm.getId("plasmid B"));
		
		try {
			int id = hsm.getId("plasmid C");
			fail("Exception expected, got " + id + " instead.");
		}
		catch(Exception e) {
			// expected
			log.info("expected: " + e.getMessage());
		}
	}

	@Test
	public void testRoman() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("chr1", 1);
		map.put("chr2", 2);
		map.put("chr3", 3);
		map.put("chr4", 4);
		map.put("chr5", 5);
		map.put("chr6", 6);
		map.put("chr7", 7);
		map.put("chr8", 8);
		map.put("chr9", 9);
		map.put("chr10", 10);
		map.put("chr11", 11);
		map.put("chr12", 12);
		map.put("chr13", 13);
		map.put("chr14", 14);
		map.put("chr15", 15);
		map.put("chr16", 16);
		map.put("chrM", 17);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("chrI"));
		assertEquals(2, hsm.getId("chr-II"));
		assertEquals(4, hsm.getId("chromosome IV"));
		assertEquals(5, hsm.getId("V"));
		assertEquals(6, hsm.getId("chromosome_VI"));
		assertEquals(7, hsm.getId("chr VII"));
		assertEquals(9, hsm.getId("chromosomeIX"));
		assertEquals(10, hsm.getId("chromosome-X"));
		assertEquals(11, hsm.getId("XI"));
		assertEquals(12, hsm.getId("chromosome_XII"));

		map = new HashMap<String, Integer>();
		map.put("I", 1);
		map.put("II", 2);
		map.put("III", 3);
		map.put("IV", 4);
		map.put("V", 5);
		map.put("VI", 6);
		map.put("VII", 7);
		map.put("VIII", 8);
		map.put("IX", 9);
		map.put("X", 10);
		map.put("XI", 11);
		map.put("XII", 12);
		map.put("XIII", 13);
		map.put("XIV", 14);
		map.put("XV", 15);
		map.put("XVI", 16);
		map.put("chrM", 17);

		hsm = new HeuristicSequenceMapper(map);

		assertEquals(1, hsm.getId("chrI"));
		assertEquals(2, hsm.getId("chr-II"));
		assertEquals(4, hsm.getId("chromosome IV"));
		assertEquals(5, hsm.getId("V"));
		assertEquals(6, hsm.getId("chromosome_VI"));
		assertEquals(7, hsm.getId("chr VII"));
		assertEquals(9, hsm.getId("chromosomeIX"));
		assertEquals(10, hsm.getId("chromosome-X"));
		assertEquals(11, hsm.getId("XI"));
		assertEquals(12, hsm.getId("chromosome_XII"));

		assertEquals(1, hsm.getId("1"));
		assertEquals(2, hsm.getId("chr2"));
		assertEquals(3, hsm.getId("chromosome3"));
		assertEquals(5, hsm.getId("chr-5"));
		assertEquals(7, hsm.getId("chromosome 7"));
		assertEquals(11, hsm.getId("11"));
		assertEquals(13, hsm.getId("13"));

	}

	@Test
	public void testUnknownPlasmid() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("1", 1);
		map.put("2", 2);
		map.put("3", 3);

		HeuristicSequenceMapper hsm = new HeuristicSequenceMapper(map);
		
		try {
			int id = hsm.getId("plasmid 1");
			fail("Exception expected, got " + id + " instead.");
		}
		catch(Exception e) {
			// expected
			log.info("expected: " + e.getMessage());
		}
	}
	
}
