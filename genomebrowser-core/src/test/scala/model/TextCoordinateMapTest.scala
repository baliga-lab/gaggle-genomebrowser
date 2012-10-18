package org.systemsbiology.genomebrowser.model

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TextCoordinateMapSpec extends FlatSpec with ShouldMatchers {

  "TextCoordinateMap" should "be created" in {
		val cm = new TextCoordinateMap
		val coords1 = cm.getCoordinates("foobar:12345000-12345678")
		coords1.getSeqId should be ("foobar")
		coords1.getStrand should be (Strand.forward)
		coords1.getStart should be (12345000)
		coords1.getEnd should be (12345678)

		val coords2 = cm.getCoordinates("chromosome-12:20123000-19999000");
		coords2.getSeqId should be ("chromosome-12")
		coords2.getStrand should be (Strand.reverse)
		coords2.getStart should be (19999000)
		coords2.getEnd should be (20123000)

		val coords3 = cm.getCoordinates("chromosome-12+:20123000-19999000");
		coords3.getSeqId should be ("chromosome-12")
		coords3.getStrand should be (Strand.forward)
		coords3.getStart should be (19999000)
		coords3.getEnd should be (20123000)

		val coords4 = cm.getCoordinates("chromosome-12+:19999000-20123000");
		coords4.getSeqId should be ("chromosome-12")
		coords4.getStrand should be (Strand.forward)
		coords4.getStart should be (19999000)
		coords4.getEnd should be (20123000)

		val coords5 = cm.getCoordinates("chromosome-12-:20123000-19999000");
		coords5.getSeqId should be ("chromosome-12")
		coords5.getStrand should be (Strand.reverse)
		coords5.getStart should be (19999000)
		coords5.getEnd should be (20123000)

		val coords6 = cm.getCoordinates("chromosome-12-:19999000-20123000");
		coords6.getSeqId should be ("chromosome-12")
		coords6.getStrand should be (Strand.reverse)
		coords6.getStart should be (19999000)
		coords6.getEnd should be (20123000)
  }
}
