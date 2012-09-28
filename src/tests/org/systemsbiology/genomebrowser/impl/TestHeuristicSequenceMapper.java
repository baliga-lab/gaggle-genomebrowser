package org.systemsbiology.genomebrowser.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.systemsbiology.util.Roman;


public class TestHeuristicSequenceMapper {

	@Test
	public void testHalo() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("pNRC200");
		sequenceNames.add("pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);
		
		assertEquals("chromosome", mapper.map("chromosome"));
		assertEquals("chromosome", mapper.map("Chromosome"));
		assertEquals("chromosome", mapper.map("ChRoMoSOME"));
		assertEquals("chromosome", mapper.map("ChRoMoSOME1"));
		assertEquals("chromosome", mapper.map("ChRoMoSOME_1"));
		assertEquals("chromosome", mapper.map("ChRoMoSOME 1"));
		assertEquals("chromosome", mapper.map("chr"));
		assertEquals("chromosome", mapper.map("chr1"));
		assertEquals("chromosome", mapper.map("chr 1"));
		assertEquals("chromosome", mapper.map("chr_1"));
		assertEquals("chromosome", mapper.map("1"));

		assertEquals("pNRC200", mapper.map("pnrc200"));
		assertEquals("pNRC200", mapper.map("plasmid pnrc200"));
		assertEquals("pNRC200", mapper.map("plasmid_pnrc200"));
		assertEquals("pNRC200", mapper.map("PNRC200"));
		assertEquals("pNRC200", mapper.map("pNRC200"));

		assertEquals("pNRC100", mapper.map("pNRC100"));
		assertEquals("pNRC100", mapper.map("Plasmid pNRC100"));		
	}

	@Test
	public void testNotFound() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("pNRC200");
		sequenceNames.add("pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);

		try {
			assertNull(mapper.map("asdfasdf"));
			fail("Should have generated an exception");
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testChr() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("pNRC200");
		sequenceNames.add("pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);

		assertEquals("chromosome", mapper.map("chr"));
		assertEquals("chromosome", mapper.map("chromosome"));
		assertEquals("chromosome", mapper.map("chromosome-1"));
		assertEquals("chromosome", mapper.map("chr1"));
		assertEquals("chromosome", mapper.map("CHR1"));
		assertEquals("chromosome", mapper.map("CHR"));
		assertEquals("chromosome", mapper.map("Chromosome"));
	}

	@Test
	public void testPlasmid() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("pNRC200");
		sequenceNames.add("pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);

		assertEquals("pNRC200", mapper.map("plasmid pNRC200"));
		assertEquals("pNRC200", mapper.map("plasmid_pNRC200"));
	}
	
	@Test
	public void testPlasmid2() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("plasmid pNRC200");
		sequenceNames.add("plasmid pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);

		assertEquals("plasmid pNRC200", mapper.map("plasmid pNRC200"));
		assertEquals("plasmid pNRC200", mapper.map("plasmid_pNRC200"));
		assertEquals("plasmid pNRC200", mapper.map("pNRC200"));
		assertEquals("plasmid pNRC200", mapper.map("pnrc200"));
		assertEquals("plasmid pNRC200", mapper.map("PNRC200"));
	}

	@Test
	public void testRomanMixedCase() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("chromosome");
		sequenceNames.add("pNRC200");
		sequenceNames.add("pNRC100");

		mapper.setStandardSequenceNames(sequenceNames);

		assertEquals("chromosome", mapper.map("ChRoMoSOME I"));
	}

	@Test
	public void testRoman() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		for (int i=1; i<=16; i++) {
			sequenceNames.add(Roman.intToRoman(i));
		}
		sequenceNames.add("chrM");
		sequenceNames.add("2micron");

		mapper.setStandardSequenceNames(sequenceNames);
		
		assertEquals("I", mapper.map("chrI"));
		assertEquals("II", mapper.map("chrII"));
		assertEquals("III", mapper.map("chrIII"));
		assertEquals("IV", mapper.map("chrIV"));
		assertEquals("V", mapper.map("chrV"));
		assertEquals("VI", mapper.map("chrVI"));
		assertEquals("VII", mapper.map("chrVII"));
		assertEquals("VIII", mapper.map("chrVIII"));
		assertEquals("IX", mapper.map("chrIX"));
		assertEquals("X", mapper.map("chrX"));
		assertEquals("XI", mapper.map("chrXI"));
		assertEquals("XII", mapper.map("chrXII"));
		assertEquals("XIII", mapper.map("chrXIII"));
		assertEquals("XIV", mapper.map("chrXIV"));
		assertEquals("XV", mapper.map("chrXV"));
		assertEquals("XVI", mapper.map("chrXVI"));
		assertEquals("chrM", mapper.map("m"));
		assertEquals("2micron", mapper.map("2micron"));
		
		assertEquals("I", mapper.map("chr1"));
		assertEquals("II", mapper.map("chr2"));
		assertEquals("III", mapper.map("chr3"));
		assertEquals("IV", mapper.map("chr4"));
		assertEquals("V", mapper.map("chr5"));
		assertEquals("VI", mapper.map("chr6"));
		assertEquals("VII", mapper.map("chr7"));
		assertEquals("VIII", mapper.map("chr8"));
		assertEquals("IX", mapper.map("chr9"));
		assertEquals("X", mapper.map("chr10"));
		assertEquals("XI", mapper.map("chr11"));
		assertEquals("XII", mapper.map("chr12"));
		assertEquals("XIII", mapper.map("chr13"));
		assertEquals("XIV", mapper.map("chr14"));
		assertEquals("XV", mapper.map("chr15"));
		assertEquals("XVI", mapper.map("chr16"));
		
		assertEquals("II", mapper.map("chr-II"));

	}

	@Test
	public void testRoman2() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		for (int i=1; i<=16; i++) {
			sequenceNames.add("chr" + i);
		}
		sequenceNames.add("chrM");
		sequenceNames.add("2micron");

		mapper.setStandardSequenceNames(sequenceNames);
		
		assertEquals("chr1", mapper.map("I"));
		assertEquals("chr2", mapper.map("II"));
		assertEquals("chr3", mapper.map("III"));
		assertEquals("chr4", mapper.map("IV"));
		assertEquals("chr5", mapper.map("V"));
		assertEquals("chr6", mapper.map("VI"));
		assertEquals("chr7", mapper.map("VII"));
		assertEquals("chr8", mapper.map("VIII"));
		assertEquals("chr9", mapper.map("IX"));
		assertEquals("chr10", mapper.map("X"));
		assertEquals("chr11", mapper.map("XI"));
		assertEquals("chr12", mapper.map("XII"));
		assertEquals("chr13", mapper.map("XIII"));
		assertEquals("chr14", mapper.map("XIV"));
		assertEquals("chr15", mapper.map("XV"));
		assertEquals("chr16", mapper.map("XVI"));
		assertEquals("chrM", mapper.map("M"));
		
		assertEquals("chr2", mapper.map("chromosome II"));
		assertEquals("chr4", mapper.map("chrIV"));
		assertEquals("chr9", mapper.map("chr-IX"));
		assertEquals("chr11", mapper.map("Chromosome_XI"));
	}

	@Test
	public void testSuffixes() {
		HeuristicSequenceMapper mapper = new HeuristicSequenceMapper();
		
		List<String> sequenceNames = new ArrayList<String>();
		sequenceNames.add("Chromosome 1");
		sequenceNames.add("Chromosome 2");
		sequenceNames.add("Chromosome 3");
		sequenceNames.add("Plasmid 1");
		sequenceNames.add("Plasmid 2");
		sequenceNames.add("Plasmid 3");
		sequenceNames.add("Plasmid A");

		mapper.setStandardSequenceNames(sequenceNames);
		
		assertEquals("Chromosome 1", mapper.map("chr1"));
		assertEquals("Chromosome 1", mapper.map("chromosome1"));
		assertEquals("Chromosome 1", mapper.map("chromosome 1"));
		assertEquals("Chromosome 1", mapper.map("1"));
		assertEquals("Chromosome 2", mapper.map("chr2"));
		assertEquals("Chromosome 3", mapper.map("chromosome3"));

		assertEquals("Plasmid 1", mapper.map("plasmid1"));
		assertEquals("Plasmid 2", mapper.map("plasmid_2"));
		assertEquals("Plasmid 3", mapper.map("plasmid 3"));
		assertEquals("Plasmid A", mapper.map("plasmid A"));
		assertEquals("Plasmid A", mapper.map("A"));
	}
}
