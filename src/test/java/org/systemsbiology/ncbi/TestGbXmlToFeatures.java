package org.systemsbiology.ncbi;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.ncbi.GbXmlSaxParser.GbXmlParseException;
import org.systemsbiology.util.FileUtils;


public class TestGbXmlToFeatures {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void testHaloChromosome() throws Exception {
		GbXmlSaxParser gb = new GbXmlSaxParser();
		List<GeneFeatureImpl> features = gb.extractFeatures(FileUtils.getInputStreamFor("classpath:/halo.chromosome.sequences.gb.xml"));
		log.info("found " + features.size() + " features");
		for (GeneFeatureImpl feature: features) {
			log.debug(feature);
		}
	}

	@Test
	public void testYeastChr1() throws Exception {
		GbXmlSaxParser gb = new GbXmlSaxParser();
		List<GeneFeatureImpl> features = gb.extractFeatures(FileUtils.getInputStreamFor("classpath:/yeast.chr1.gb.xml"));
		log.info("found " + features.size() + " features");

		// 94 coding sequences, 6 rnas
		assertEquals(100, features.size());

		GeneFeatureImpl feature = features.get(0);
		assertEquals("YAL068C", feature.getName());
		assertEquals("PAU8", feature.getCommonName());
		assertEquals(1807, feature.getStart());
		assertEquals(2169, feature.getEnd());
		assertEquals(Strand.reverse, feature.getStrand());

		// YAL030W has an intron
		feature = findFeatureByName("YAL030W", features);
		assertEquals(87287, feature.getStart());
		assertEquals(87753, feature.getEnd());
	}

	public GeneFeatureImpl findFeatureByName(String name, List<GeneFeatureImpl> features) {
		for (GeneFeatureImpl feature : features) {
			if (name.equals(feature.getName()))
				return feature;
		}
		return null;
	}

	@Test
	public void testHumanChr1() throws Exception {
		GbXmlSaxParser gb = new GbXmlSaxParser();
		try {
			gb.extractFeatures(FileUtils.getInputStreamFor("classpath:/human.chr1.gb.xml"));
			fail("Expected and exception while parsing human gb.xml file");
		}
		catch (GbXmlParseException e) {
			// parsing should throw an exception due to the fact that I don't want
			// to write a parser for GBSeq_contig elements.
			assertTrue(e.getMessage().contains("GBSeq_contig"));
		}
	}
}
