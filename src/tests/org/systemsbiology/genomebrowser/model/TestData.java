package org.systemsbiology.genomebrowser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.impl.BasicDataset;
import org.systemsbiology.genomebrowser.impl.BasicQuantitativeBlock;
import org.systemsbiology.genomebrowser.impl.BasicSequence;
import org.systemsbiology.genomebrowser.impl.Block;
import org.systemsbiology.genomebrowser.impl.FeatureBlock;
import org.systemsbiology.genomebrowser.impl.GeneTrack;
import org.systemsbiology.genomebrowser.impl.QuantitativeTrack;
import org.systemsbiology.util.Pair;


/**
 * Create a bogus dataset that can be used for testing
 */
public class TestData {
	private static final Logger log = Logger.getLogger(TestData.class);
	private static final int BLOCK_SIZE = 2000; 

	public Dataset createTestDataset() {
		List<Sequence> sequences = createSequences();

		List<Track<? extends Feature>> tracks = new ArrayList<Track<? extends Feature>>();
		tracks.add(createGeneTrack(sequences));
		tracks.add(createQuantitativeTrack(sequences, "waves", 1000));

		BasicDataset dataset = new BasicDataset();
		dataset.setName("Test Dataset");
		dataset.setUuid(UUID.fromString("39b9f460-802b-11dd-ad8b-0800200c9a66"));
		dataset.setSequences(sequences);
		dataset.addTracks(tracks);

		log.info("created test dataset: " + dataset);

		return dataset;
	}

	List<Sequence> createSequences() {
		List<Sequence> sequences = new ArrayList<Sequence>();
		sequences.add(new BasicSequence(UUID.randomUUID(), "chromosome", 4000000, Topology.circular));
		sequences.add(new BasicSequence(UUID.randomUUID(), "plasmid I",   400000, Topology.circular));
		sequences.add(new BasicSequence(UUID.randomUUID(), "plasmid II",  150000, Topology.circular));
		return sequences;
	}

	GeneTrack<GeneFeatureImpl> createGeneTrack(List<Sequence> sequences) {
		GeneTrack<GeneFeatureImpl> genes = new GeneTrack<GeneFeatureImpl>(UUID.randomUUID(), "genome");

		Pair<Block<GeneFeatureImpl>, Block<GeneFeatureImpl>> blocks = createGeneFeatures(sequences.get(0), 1800, "a");
		genes.addGeneFeatures(new FeatureFilter(sequences.get(0), Strand.forward), blocks.getFirst());
		genes.addGeneFeatures(new FeatureFilter(sequences.get(0), Strand.reverse), blocks.getSecond());

		blocks = createGeneFeatures(sequences.get(1), 1500, "b");
		genes.addGeneFeatures(new FeatureFilter(sequences.get(1), Strand.forward), blocks.getFirst());
		genes.addGeneFeatures(new FeatureFilter(sequences.get(1), Strand.reverse), blocks.getSecond());

		blocks = createGeneFeatures(sequences.get(2), 1200, "c");
		genes.addGeneFeatures(new FeatureFilter(sequences.get(2), Strand.forward), blocks.getFirst());
		genes.addGeneFeatures(new FeatureFilter(sequences.get(2), Strand.reverse), blocks.getSecond());

		genes.getAttributes().put("top", "0.42");
		genes.getAttributes().put("height", "0.16");
		genes.getAttributes().put("viewer", "Gene");
		return genes;
	}

	public Pair<Block<GeneFeatureImpl>,Block<GeneFeatureImpl>> createGeneFeatures(Sequence sequence, int geneLength, String prefix) {
		List<GeneFeatureImpl> forwardFeatures = new ArrayList<GeneFeatureImpl>();
		List<GeneFeatureImpl> reverseFeatures = new ArrayList<GeneFeatureImpl>();
		int start = 1;
		int end = geneLength;
		int i = 1;

		Strand strand = Strand.forward;
		while (end < sequence.getLength()) {
			String name = String.format("%s%04d",prefix,i);
			if (strand == Strand.forward)
				forwardFeatures.add(new GeneFeatureImpl(sequence.getSeqId(), strand, start, end, name, GeneFeatureType.cds));
			else
				reverseFeatures.add(new GeneFeatureImpl(sequence.getSeqId(), strand, start, end, name, GeneFeatureType.cds));
			start = end + 50;
			end = start + geneLength;
			i++;
			strand = Strand.opposite(strand);
		}

		return new Pair<Block<GeneFeatureImpl>,Block<GeneFeatureImpl>>(
				new FeatureBlock<GeneFeatureImpl>(sequence, Strand.forward, forwardFeatures),
				new FeatureBlock<GeneFeatureImpl>(sequence, Strand.reverse, reverseFeatures));
	}

	public QuantitativeTrack createQuantitativeTrack(List<Sequence> sequences, String name, double period) {
		QuantitativeTrack track = new QuantitativeTrack(UUID.randomUUID(), name);
		for (Sequence sequence: sequences) {
			for (Strand strand : Strand.both) {
				int n = (int)((sequence.getLength() - 60.0) / 20.0);

				int i=0;
				while (i<n) {
					// init arrays for this block
					int blockSize = Math.min(BLOCK_SIZE, n-i);
					int[] starts = new int[blockSize];
					int[] ends   = new int[blockSize];
					double[] values = new double[blockSize];

					// compute values for this block
					for (int b=0; b<blockSize; b++,i++) {
						starts[b] = i*20+1;
						ends[b] = starts[b]+59;
						values[b] = strand==Strand.forward ? 
													Math.sin(2*Math.PI * i/period) :
													Math.cos(2*Math.PI * i/period);
					}
					int start = starts[0];
					int end = ends[ends.length-1];

					// store block in track
					track.putFeatures(
							new FeatureFilter(sequence, strand, start, end),
							new BasicQuantitativeBlock<Feature.Quantitative>(sequence, strand, starts, ends, values));
				}
			}
		}
		track.getAttributes().put("top", "0.10");
		track.getAttributes().put("height", "0.20");
		track.getAttributes().put("viewer", "Scaling");
		return track;
	}
}
