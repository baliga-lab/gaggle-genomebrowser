package org.systemsbiology.genomebrowser.model

import java.util.{ArrayList, Iterator, List, UUID}

import org.systemsbiology.genomebrowser.model._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeneTrackSpec extends FlatSpec with ShouldMatchers {

  "GeneTrack" should "be tested" in {
    // setup
		val track = new GeneTrack[GeneFeature](UUID.randomUUID, "Moose Genome")
		val chromosomeI = new BasicSequence(UUID.randomUUID, "I", 10100, Topology.circular)
		val chromosomeII = new BasicSequence(UUID.randomUUID, "II", 10100, Topology.circular)

		val genes1 = new ArrayList[GeneFeature]
		for (i <- 0 until 100) {
			genes1.add(new GeneFeatureImpl("I", Strand.forward, i * 100, i * 100 + 90, "m%04d".format(i),
                                     GeneFeatureType.cds))
		}
		track.addGeneFeatures(new FeatureBlock[GeneFeature](chromosomeI, Strand.forward, genes1))

		val genes2 = new ArrayList[GeneFeature]
		for (i <- 0 until 100) {
			genes2.add(new GeneFeatureImpl("II", Strand.forward, i * 100, i * 100 + 90, "x%04d".format(i),
                                     GeneFeatureType.cds))
		}
		track.addGeneFeatures(new FeatureBlock[GeneFeature](chromosomeII, Strand.forward, genes2))

    // test
		val features1 = track.features(new FeatureFilter(chromosomeI, Strand.forward, 500, 700))
		features1.hasNext should be (true)
		val feature1_1 = features1.next
		feature1_1.getName should be ("m0005")
		features1.hasNext should be (true)
		val feature1_2 = features1.next
		feature1_2.getName should be ("m0006")
		features1.hasNext should be (false)

		val features2 = track.features(new FeatureFilter(chromosomeII, Strand.forward, 500, 700))
		features2.hasNext should be (true)
		val feature2_1 = features2.next
		feature2_1.getName should be ("x0005")
		features2.hasNext should be (true)
		val feature2_2 = features2.next();
		feature2_2.getName should be ("x0006")
		features2.hasNext should be (false)
  }
}
