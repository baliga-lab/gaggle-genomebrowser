package org.systemsbiology.genomebrowser.impl;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Topology;


public class TestQuantitativeTrack {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		Sequence seq = new BasicSequence(UUID.randomUUID(), "MySeq", 100000, Topology.circular);
		QuantitativeTrack track = new QuantitativeTrack("Test Track");

		Strand strand = Strand.forward;
		Block<Feature.Quantitative> block = makeBlock(0, seq, strand);
		track.putFeatures(new FeatureFilter(seq, strand), block);

		strand = Strand.reverse;
		block = makeBlock(1, seq, strand);
		track.putFeatures(new FeatureFilter(seq, strand), block);

		int i=0;
		for (Feature.Quantitative fq : track.features(new FeatureFilter(seq, Strand.forward, 250, 850))) {
			log.info(fq);
			i++;
		}
		assertEquals(4, i);

		for (Feature.Quantitative fq : track.features(new FeatureFilter(seq, Strand.reverse, 1301, 1402))) {
			log.info(fq);
			assertEquals(1401, fq.getStart());
			assertEquals(1500, fq.getEnd());
			assertEquals(0.875, fq.getValue(), 0.001);
		}
	}

	/**
	 * create a block of 5 quantitative features.
	 */
	private Block<Feature.Quantitative> makeBlock(int i, Sequence seq, Strand strand) {
		int offset = 1000 * i;
		int[] starts = new int[] {1+offset, 201+offset, 401+offset, 601+offset, 801+offset};
		int[] ends   = new int[] {100+offset, 300+offset, 500+offset, 700+offset, 900+offset};
		double[] values = new double[] {0.5, 0.75, 0.875, 0.9375, 0.96875};
		BasicQuantitativeBlock<Feature.Quantitative> block = new BasicQuantitativeBlock<Feature.Quantitative>(
				seq,
				strand,
				starts, ends, values);
		return block;
	}
}
