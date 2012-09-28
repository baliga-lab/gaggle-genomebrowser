package org.systemsbiology.genomebrowser.impl;

import static org.junit.Assert.*;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Topology;


public class TestBasicQuantitativeBlock {
	private static final Logger log = Logger.getLogger("unit-test");

	@Test
	public void test() {
		int[] starts = new int[] {1, 201, 401, 601, 801};
		int[] ends   = new int[] {100, 300, 500, 700, 900};
		double[] values = new double[] {0.5, 0.75, 0.875, 0.9375, 0.96875};
		BasicQuantitativeBlock<Feature.Quantitative> block = new BasicQuantitativeBlock<Feature.Quantitative>(
				new BasicSequence(UUID.randomUUID(), "MySeq", 100000, Topology.circular),
				Strand.forward,
				starts, ends, values);
		
		int i=0;
		for (Feature.Quantitative fq : block) {
			log.info(fq);
			assertEquals("MySeq", fq.getSeqId());
			assertEquals(Strand.forward, fq.getStrand());
			assertEquals(starts[i], fq.getStart());
			assertEquals(ends[i], fq.getEnd());
			assertEquals(values[i], fq.getValue(), 0.001);
			i++;
		}

		// block should have 5 features
		assertEquals(5, i);

		i=2;
		for (Feature.Quantitative fq : block.features(400, 700)) {
			assertEquals(starts[i], fq.getStart());
			assertEquals(ends[i], fq.getEnd());
			assertEquals(values[i], fq.getValue(), 0.001);
			i++;
		}

		// should iterate features i=2,(401,500) and i=3,(601,700)
		assertEquals(4, i);
	}
}
