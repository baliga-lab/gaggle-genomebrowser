package org.systemsbiology.genomebrowser.model

import java.util.UUID

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BasicQuantitativeBlockSpec extends FlatSpec with ShouldMatchers {

  "BasicQuantitativeBlock" should "be created" in {
		val starts = Array(1, 201, 401, 601, 801)
		val ends   = Array(100, 300, 500, 700, 900)
		val values = Array(0.5, 0.75, 0.875, 0.9375, 0.96875)
		val block = new BasicQuantitativeBlock[Feature#Quantitative](
			new BasicSequence(UUID.randomUUID, "MySeq", 100000, Topology.circular),
			Strand.forward,	starts, ends, values)

		var numFeatures1 = 0
    val blockFeatures1 = block.iterator
		while (blockFeatures1.hasNext) {
      val fq = blockFeatures1.next
      fq.getSeqId should be ("MySeq")
      fq.getStrand should be (Strand.forward)
      fq.getStart should be (starts(numFeatures1))
      fq.getEnd should be (ends(numFeatures1))
      fq.getValue should be (values(numFeatures1))
			numFeatures1 += 1
		}

		// block should have 5 features
    numFeatures1 should be (5)

		// should iterate features i=2,(401,500) and i=3,(601,700)
    var numFeatures2 = 0
    val blockFeatures2 = block.features(400, 700)
		while (blockFeatures2.hasNext) {
      val fq = blockFeatures2.next
      fq.getStart should be (starts(2 + numFeatures2))
      fq.getEnd should be (ends(2 + numFeatures2))
      fq.getValue should be (values(2 + numFeatures2))
      numFeatures2 += 1
    }
    numFeatures2 should be (2)
  }
}
