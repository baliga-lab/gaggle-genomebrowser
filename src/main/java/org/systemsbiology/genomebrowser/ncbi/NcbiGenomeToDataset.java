package org.systemsbiology.genomebrowser.ncbi;

import java.util.List;
import java.util.UUID;

import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.FeatureFields;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.ncbi.NcbiGenome;
import org.systemsbiology.ncbi.NcbiSequence;
import org.systemsbiology.util.ProgressListener;

/**
 * Takes an NcbiGenome object and creates a dataset representing
 * the sequences and a single genome track.
 */
public class NcbiGenomeToDataset {
	DatasetBuilder datasetBuilder;

	public NcbiGenomeToDataset(DatasetBuilder datasetBuilder) {
		this.datasetBuilder = datasetBuilder;
	}

	public Dataset convert(final NcbiGenome genome) {
		UUID dsuuid = datasetBuilder.beginNewDataset(genome.getOrganismName());
		datasetBuilder.setAttribute(dsuuid, "species",genome.getOrganismName());
		datasetBuilder.setAttribute(dsuuid, "group", genome.getGroup());
		datasetBuilder.setAttribute(dsuuid, "superkingdom", genome.getSuperkingdom());
		datasetBuilder.setAttribute(dsuuid, "ncbi.project.id", genome.getProjectId());

		for (NcbiSequence ncbiSequence: genome.getSequences()) {
			UUID sequuid = datasetBuilder.addSequence(ncbiSequence.getName(), ncbiSequence.getLength(), Topology.fromString(ncbiSequence.getTopology()));
			datasetBuilder.setAttribute(sequuid, "accession", ncbiSequence.getAccession());
			datasetBuilder.setAttribute(sequuid, "ncbi.definition", ncbiSequence.getDefinition());
			datasetBuilder.setAttribute(sequuid, "ncbi.update.date", ncbiSequence.getUpdateDate());
			datasetBuilder.setAttribute(sequuid, "locus", ncbiSequence.getLocus());
			datasetBuilder.setAttribute(sequuid, "ncbi.id", ncbiSequence.getNcbiId());
		}

		FeatureSource featureSource = new FeatureSource() {
			public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
				GeneFeatureFields fields = new GeneFeatureFields();
				for (NcbiSequence ncbiSequence: genome.getSequences()) {
					List<GeneFeatureImpl> genes = ncbiSequence.getGenes();
					for (GeneFeatureImpl gene: genes) {
						fields.gene = gene;
						featureProcessor.process(fields);
					}
				}
			}
			public void addProgressListener(ProgressListener progressListener) {}
			public void removeProgressListener(ProgressListener progressListener) {}
		};
		UUID tuuid = datasetBuilder.addTrack("gene", "Genome", featureSource);
		datasetBuilder.setAttribute(tuuid, "top", "0.42");
		datasetBuilder.setAttribute(tuuid, "height", "0.16");
		datasetBuilder.setAttribute(tuuid, "viewer", "Gene");

		return datasetBuilder.getDataset();
	}

	private static class GeneFeatureFields implements FeatureFields {
		GeneFeatureImpl gene;

		public String getCommonName() {
			return gene.getCommonName();
		}

		public int getEnd() {
			return gene.getEnd();
		}

		public String getGeneType() {
			return gene.getType().toString();
		}

		public String getName() {
			return gene.getName();
		}

		public int getPosition() {
			return gene.getCentralPosition();
		}

		public String getSequenceName() {
			return gene.getSeqId();
		}

		public int getStart() {
			return gene.getStart();
		}

		public String getStrand() {
			return gene.getStrand().toAbbreviatedString();
		}

		public double getValue() {
			return 0.0;
		}
		
		public String toString() {
			return String.format("%s(%s): %s%s:%d-%d %.2f",
					getName(), getGeneType(),
					getSequenceName(), getStrand(), getStart(), getEnd(),
					getValue());
		}
	}
}
