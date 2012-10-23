package org.systemsbiology.genomebrowser.model

import java.util.UUID

import org.systemsbiology.genomebrowser.model._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class QuantitativeTrackSpec extends FlatSpec with ShouldMatchers {

	// create a block of 5 quantitative features.
	private def makeBlock(i: Int, seq: Sequence, strand: Strand): Block[Feature#Quantitative] = {
		val offset = 1000 * i
		val starts = Array(1 + offset, 201 + offset, 401 + offset, 601 + offset, 801 + offset)
		val ends   = Array(100 + offset, 300 + offset, 500 + offset, 700 + offset, 900 + offset)
		val values = Array(0.5, 0.75, 0.875, 0.9375, 0.96875)
		new BasicQuantitativeBlock[Feature#Quantitative](seq, strand, starts, ends, values)
	}

  "QuantitativeTrack" should "be created" in {
		val seq = new BasicSequence(UUID.randomUUID, "MySeq", 100000, Topology.circular)
		val track = new QuantitativeTrack("Test Track")

		val strand1 = Strand.forward
		val block1 = makeBlock(0, seq, strand1)
		track.putFeatures(new FeatureFilter(seq, strand1), block1)

		val strand2 = Strand.reverse;
		val block2 = makeBlock(1, seq, strand2);
		track.putFeatures(new FeatureFilter(seq, strand2), block2);

		val trackFeatures1 = track.features(new FeatureFilter(seq, Strand.forward, 250, 850))
    var numTrackFeatures1 = 0
    while (trackFeatures1.hasNext) {
      numTrackFeatures1 += 1
      trackFeatures1.next
    }
    numTrackFeatures1 should be (4)

		val trackFeatures2 = track.features(new FeatureFilter(seq, Strand.reverse, 1301, 1402))
    var numTrackFeatures2 = 0
    while (trackFeatures2.hasNext) {
      val fq = trackFeatures2.next
			fq.getStart should be (1401)
			fq.getEnd should be (1500)
			fq.getValue should be (0.875)
      numTrackFeatures2 += 1
    }
    numTrackFeatures2 should be (1)
  }
}
