package org.systemsbiology.genomebrowser.visualization

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Test our ability to convert between screen and genome coordinates.
 */
@RunWith(classOf[JUnitRunner])
class ViewParametersSpec extends FlatSpec with ShouldMatchers {
  "ViewParameters" should "convert screen coordinates to genome coordinates and back" in {
		val vp = new ViewParameters
		vp.setDeviceSize(800, 500)
		vp.setRange(1, 20000)

		// do a round trip from screen coordinate to genome coordinate and back
		// there is potential for round-off error here, but this seems to work
		vp.toScreenX(vp.toGenomeCoordinate(400)) should be (400)
  }
  it should "convert within small tolerances" in {
		val vp = new ViewParameters
		vp.setDeviceSize(800, 500)
		vp.setRange(1, 20000)

		// since 20000/800 = 25, the error in converting to the nearest
		// pixel and back should be no greater than 25/2 ~= 12.
		for (genomeX <- 10001 until 20000 by 37) {
			val x = vp.toScreenX(genomeX);
			val gx = vp.toGenomeCoordinate(x);

			// These won't generally be exactly equal because we loose
			// resolution going from genome to screen coordinates
      assert(math.abs(genomeX - gx) < 13.0)
    }
  }
}
