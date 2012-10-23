package org.systemsbiology.genomebrowser.model

import java.util.{ArrayList, List, UUID}
import scala.collection.JavaConversions._

import org.systemsbiology.genomebrowser.model._
import org.systemsbiology.util.Pair

/**
 * Create a bogus dataset that can be used for testing
 */
class TestData {
	private val BLOCK_SIZE = 2000

	def createTestDataset: Dataset = {
		val dataset = new BasicDataset
		val sequences = createSequences
		val tracks = new ArrayList[Track[_ <: Feature]]

		tracks.add(createGeneTrack(sequences))
		tracks.add(createQuantitativeTrack(sequences, "waves", 1000))

		dataset.setName("Test Dataset")
		dataset.setUuid(UUID.fromString("39b9f460-802b-11dd-ad8b-0800200c9a66"))
		dataset.setSequences(sequences)
		dataset.addTracks(tracks)
 		dataset
	}

	private def createSequences: List[Sequence] = {
		val sequences = new ArrayList[Sequence]
		sequences.add(new BasicSequence(UUID.randomUUID, "chromosome", 4000000, Topology.circular))
		sequences.add(new BasicSequence(UUID.randomUUID, "plasmid I",   400000, Topology.circular))
		sequences.add(new BasicSequence(UUID.randomUUID, "plasmid II",  150000, Topology.circular))
		sequences
	}

	def createGeneTrack(sequences: List[Sequence]): GeneTrack[GeneFeatureImpl] = {
		val genes = new GeneTrack[GeneFeatureImpl](UUID.randomUUID, "genome")

		val blocks1 = createGeneFeatures(sequences.get(0), 1800, "a")
		genes.addGeneFeatures(new FeatureFilter(sequences.get(0), Strand.forward), blocks1.getFirst)
		genes.addGeneFeatures(new FeatureFilter(sequences.get(0), Strand.reverse), blocks1.getSecond)

		val blocks2 = createGeneFeatures(sequences.get(1), 1500, "b")
		genes.addGeneFeatures(new FeatureFilter(sequences.get(1), Strand.forward), blocks2.getFirst)
		genes.addGeneFeatures(new FeatureFilter(sequences.get(1), Strand.reverse), blocks2.getSecond)

		val blocks3 = createGeneFeatures(sequences.get(2), 1200, "c")
		genes.addGeneFeatures(new FeatureFilter(sequences.get(2), Strand.forward), blocks3.getFirst)
		genes.addGeneFeatures(new FeatureFilter(sequences.get(2), Strand.reverse), blocks3.getSecond)

		genes.getAttributes().put("top", "0.42")
		genes.getAttributes().put("height", "0.16")
		genes.getAttributes().put("viewer", "Gene")
		genes
	}

	private def createGeneFeatures(sequence: Sequence, geneLength: Int,
                                 prefix: String): Pair[Block[GeneFeatureImpl], Block[GeneFeatureImpl]] = {
		val forwardFeatures = new ArrayList[GeneFeatureImpl]
		val reverseFeatures = new ArrayList[GeneFeatureImpl]
		var start = 1
		var end = geneLength
		var i = 1
		var strand = Strand.forward

		while (end < sequence.getLength) {
			val name = "%s%04d".format(prefix, i)
			if (strand == Strand.forward)
				forwardFeatures.add(new GeneFeatureImpl(sequence.getSeqId, strand,
                                                start, end, name, GeneFeatureType.cds))
			else
				reverseFeatures.add(new GeneFeatureImpl(sequence.getSeqId, strand,
                                                start, end, name, GeneFeatureType.cds))
			start = end + 50
			end = start + geneLength
			i += 1
			strand = Strand.opposite(strand)
		}
		new Pair[Block[GeneFeatureImpl],Block[GeneFeatureImpl]](
			new FeatureBlock[GeneFeatureImpl](sequence, Strand.forward, forwardFeatures),
			new FeatureBlock[GeneFeatureImpl](sequence, Strand.reverse, reverseFeatures))
	}

	def createQuantitativeTrack(sequences: List[Sequence], name: String, period: Double): QuantitativeTrack = {
		val track = new QuantitativeTrack(UUID.randomUUID, name)
		for (sequence <- sequences) {
			for (strand <- Strand.both) {
				val n = ((sequence.getLength() - 60.0) / 20.0).asInstanceOf[Int]
				var i = 0
				while (i < n) {
					// init arrays for this block
					val blockSize = math.min(BLOCK_SIZE, n - i)
					val starts = Array.ofDim[Int](blockSize)
					val ends   = Array.ofDim[Int](blockSize)
					val values = Array.ofDim[Double](blockSize)

					// compute values for this block
					for (b <- 0 until blockSize) {
						starts(b) = i * 20 + 1
						ends(b) = starts(b) + 59
						values(b) = if (strand==Strand.forward) math.sin(2 * math.Pi * i / period)
                        else  math.cos(2 * math.Pi * i / period)
            i += 1
					}

					val start = starts(0)
					val end = ends(ends.length - 1)

					// store block in track
					track.putFeatures(new FeatureFilter(sequence, strand, start, end),
							              new BasicQuantitativeBlock[Feature#Quantitative](sequence, strand, starts, ends, values))
				}
			}
		}
		track.getAttributes().put("top", "0.10")
		track.getAttributes().put("height", "0.20")
		track.getAttributes().put("viewer", "Scaling")
		track
	}
}
