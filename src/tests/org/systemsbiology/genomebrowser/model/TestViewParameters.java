package org.systemsbiology.genomebrowser.model;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.systemsbiology.genomebrowser.visualization.ViewParameters;

/**
 * Test our ability to convert between screen and genome coordinates.
 */
public class TestViewParameters {
	private static final Logger log = Logger.getLogger(TestViewParameters.class);

	@Test
	public void testCoordinateConversions() {
		ViewParameters vp = new ViewParameters();
		vp.setDeviceSize(800, 500);
		vp.setRange(1, 20000);

		// do a round trip from screen coordinate to genome coordinate and back
		int screenX = 400;
		log.info("screenX = " + screenX);
		int genomeX = vp.toGenomeCoordinate(screenX);
		log.info("genomeX = " + genomeX);
		int x = vp.toScreenX(genomeX);
		log.info("computed screenX = " + x);

		// there is potential for round-off error here, but this seems to work
		assertEquals(screenX, x);
	}

	@Test
	public void testCoordinateConversions2() {
		ViewParameters vp = new ViewParameters();
		vp.setDeviceSize(800, 500);
		vp.setRange(1, 20000);

		// since 20000/800 = 25, the error in converting to the nearest
		// pixel and back should be no greater than 25/2 ~= 12.

		for (int genomeX = 10001; genomeX < 20000; genomeX += 37) {
			log.info("genomeX = " + genomeX);
			int x = vp.toScreenX(genomeX);
			log.info("computed screenX = " + x);
			int gx = vp.toGenomeCoordinate(x);
			log.info("computed genomeX = " + gx + " --- error=" + Math.abs(genomeX-gx));

			// These won't generally be exactly equal because we loose
			// resolution going from genome to screen coordinates
			assertEquals(genomeX, gx, 13.0);
		}
	}
}
