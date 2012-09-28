package org.systemsbiology.genomebrowser.sqlite;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import org.systemsbiology.genomebrowser.impl.BasicQuantitativeFeature;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;


public class TestBlockLoader {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		SqliteDataSource dataSource = new SqliteDataSource("jdbc:sqlite:test.hbgb");
		
		// load a block holding the first 5 features from the transcript signal track
		SegmentBlock block = dataSource.loadSegmentBlock(new BlockKey(
				UUID.fromString("b7c82f9e-1485-13a6-5683-98d8f23b06e0"),
				1, "chromosome", Strand.forward,
				11, 150,
				5,
				"features_transcript_signal",
				1, 5));

		// confirm that features are as expected
		List<Feature.Quantitative> expectedFeatures = getExpectedFeatures();
		for (Feature feature : block) {
			log.info(feature);
			Feature.Quantitative expectedFeature = expectedFeatures.remove(0);
			assertEquals(expectedFeature.getSeqId(), feature.getSeqId());
			assertEquals(expectedFeature.getStrand(), feature.getStrand());
			assertEquals(expectedFeature.getStart(), feature.getStart());
			assertEquals(expectedFeature.getEnd(), feature.getEnd());
		}
	}

	public List<Feature.Quantitative> getExpectedFeatures() {
		List<Feature.Quantitative> features = new LinkedList<Feature.Quantitative>();
		features.add(new BasicQuantitativeFeature("chromosome", Strand.forward, 11, 70, 12.51));
		features.add(new BasicQuantitativeFeature("chromosome", Strand.forward, 31, 90, 9.03));
		features.add(new BasicQuantitativeFeature("chromosome", Strand.forward, 51, 110, 6.84));
		features.add(new BasicQuantitativeFeature("chromosome", Strand.forward, 71, 130, 6.81));
		features.add(new BasicQuantitativeFeature("chromosome", Strand.forward, 91, 150, 5.48));
		return features;
	}
}
