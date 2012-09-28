package org.systemsbiology.genomebrowser.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Topology;


public class TestGeneTrack {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		GeneTrack<GeneFeature> track = new GeneTrack<GeneFeature>(UUID.randomUUID(), "Moose Genome");

		Sequence chromosomeI = new BasicSequence(UUID.randomUUID(), "I", 10100, Topology.circular);
		Sequence chromosomeII = new BasicSequence(UUID.randomUUID(), "II", 10100, Topology.circular);

		List<GeneFeature> genes = new ArrayList<GeneFeature>();
		for (int i=0; i<100; i++) {
			genes.add(new GeneFeatureImpl("I", Strand.forward, i*100, i*100+90, String.format("m%04d", i), GeneFeatureType.cds));
		}

		track.addGeneFeatures(new FeatureBlock<GeneFeature>(chromosomeI, Strand.forward, genes));

		genes = new ArrayList<GeneFeature>();
		for (int i=0; i<100; i++) {
			genes.add(new GeneFeatureImpl("II", Strand.forward, i*100, i*100+90, String.format("x%04d", i), GeneFeatureType.cds));
		}
		track.addGeneFeatures(new FeatureBlock<GeneFeature>(chromosomeII, Strand.forward, genes));

		Iterator<GeneFeature> features = track.features(new FeatureFilter(chromosomeI, Strand.forward, 500,700));
		assertTrue(features.hasNext());
		GeneFeature feature = features.next();
		log.info(feature);
		assertEquals("m0005", feature.getName());
		assertTrue(features.hasNext());
		feature = features.next();
		log.info(feature);
		assertEquals("m0006", feature.getName());
		assertFalse(features.hasNext());

		features = track.features(new FeatureFilter(chromosomeII, Strand.forward, 500,700));
		assertTrue(features.hasNext());
		feature = features.next();
		log.info(feature);
		assertEquals("x0005", feature.getName());
		assertTrue(features.hasNext());
		feature = features.next();
		log.info(feature);
		assertEquals("x0006", feature.getName());
		assertFalse(features.hasNext());
	}
}
