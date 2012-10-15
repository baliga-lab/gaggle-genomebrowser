package org.systemsbiology.genomebrowser.ucscgb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.event.Event;
import org.systemsbiology.genomebrowser.app.Application;
import org.systemsbiology.genomebrowser.app.ProjectDescription;
import org.systemsbiology.genomebrowser.model.BasicSequence;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.ucscgb.Category;
import org.systemsbiology.ucscgb.Chromosome;
import org.systemsbiology.ucscgb.Gene;
import org.systemsbiology.ucscgb.Genome;
import org.systemsbiology.ucscgb.UCSCGB;
import org.systemsbiology.util.ProgressListener;
import org.systemsbiology.util.ProgressListenerSupport;

// TODO needs a better name. The dataset builder is a builder. What's the name
// for the thing that drives that builder? A driver? UcscNewProjectDriver?

/**
 * Creates a dataset from a UCSC genome
 * @author cbare
 */
public class UcscDatasetBuilder {
	private static final Logger log = Logger.getLogger(UcscDatasetBuilder.class);
	private DatasetBuilder builder;
	private UCSCGB ucsc = new UCSCGB();
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();
	private boolean removeUnassembledFragments = true;


	public void drive(ProjectDescription projectDescription, Application app) {
		ucsc.addProgressListener(progressListenerSupport);
		new Thread(new CreateNewProjectTask(projectDescription, app)).start();
	}

	class CreateNewProjectTask implements Runnable {
		public ProjectDescription projectDescription;
		public Application app;

		public CreateNewProjectTask(ProjectDescription projectDescription, Application app) {
			this.projectDescription = projectDescription;
			this.app = app;
		}

		public void run() {
			try {
				app.publishEvent(new Event(UcscDatasetBuilder.this, "import genome", progressListenerSupport));
				progressListenerSupport.fireSetExpectedProgressEvent(1000);
				projectDescription.getFile().getParentFile().mkdirs();
				setDatasetBuilder(app.io.getDatasetBuilder(projectDescription.getFile()));
				setRemoveUnassembledFragments(projectDescription.getRemoveUnassembledFragments());
				Dataset dataset = createDatasetFromScientificName(projectDescription.getOrganism());
				// TODO unify new dataset code
				app.setDataset(dataset, projectDescription.getFile());
				//app.options.datasetUrl = projectDescription.getFile().getAbsolutePath();
			}
			catch (Exception e) {
				app.reportException("Error while trying to download genome from UCSC.", e);
			}
			finally {
				progressListenerSupport.fireDoneEvent();
			}
		}
	}

	public void setDatasetBuilder(DatasetBuilder builder) {
		this.builder = builder;
	}

	public void setRemoveUnassembledFragments(boolean removeUnassembledFragments) {
		this.removeUnassembledFragments = removeUnassembledFragments;
	}

	public Dataset createDatasetFromScientificName(String species) throws Exception {
		List<Genome> genomes = ucsc.loadGenomes();
		Genome genome = Genome.findByScientificName(genomes, species);
		if (genome==null)
			throw new RuntimeException("No information found for organism: " + species);
		return createDataset(genome);
	}

	public Dataset createDataset(String dbName) throws Exception {
		return createDataset(ucsc.loadGenome(Category.all, dbName));
	}

	public Dataset createDataset(Genome genome) throws Exception {
		log.info("creating dataset from genome: " + (genome==null? "null" : genome.toDebugString()));
		if (genome==null)
			throw new NullPointerException("Can't create dataset from null genome.");

		UUID datasetUuid = builder.beginNewDataset(genome.getScientificName());

		builder.setAttribute(datasetUuid, "species", genome.getScientificName());
		builder.setAttribute(datasetUuid, "created-on", new Date());
		builder.setAttribute(datasetUuid, "created-by", System.getProperty("user.name"));
		builder.setAttribute(datasetUuid, "ucsc.db.name", genome.getDbName());
		builder.setAttribute(datasetUuid, "domain", genome.getDomain());
		builder.setAttribute(datasetUuid, "ucsc.clade", genome.getClade());
		builder.setAttribute(datasetUuid, "ucsc.gene.table", genome.getGeneTable());
		if (genome.getTaxid() > 0)
			builder.setAttribute(datasetUuid, "ncbi.taxonomy.id", genome.getTaxid());

		List<Chromosome> chromosomes = ucsc.chromInfo(genome.getCategory(), genome.getDbName(), removeUnassembledFragments);

		// down at the DB level, looping through the chromosomes like this
		// turns into a problem when we have lots of sequences. This is true
		// for unassembled genomes consisting of lots of fragments like Sea
		// Urchin (Strongylocentrotus purpuratus) for example which has 114K
		// scaffolds.
//		for (Chromosome chr : chromosomes) {
//			UUID seqUuid = builder.addSequence(chr.getName(), chr.getSize(), topology);
//			builder.setAttribute(seqUuid, "ucsc.name", chr.getName());
//		}

		// add all sequences in one shot
		builder.addSequences(toSequences(chromosomes, genome.getCategory()));

		// guess expected progress based on domain of life
		progressListenerSupport.fireSetExpectedProgressEvent(getExpectedProgress(genome.getDomain()));

		if ("none".equals(genome.getGeneTable())) {
			log.warn("No gene table for genome: " + genome.toDebugString());
		}
		else {
			final List<Gene> genes = ucsc.genes(genome, removeUnassembledFragments);
			FeatureSource featureSource = new FeatureSource() {
				int progress;
				public void processFeatures(FeatureProcessor featureProcessor) throws Exception {
					GeneFeatureFields fields = new GeneFeatureFields();
					for (Gene gene: genes) {
						fields.gene = gene;
						featureProcessor.process(fields);
						reportProgress();
					}
				}
				public void addProgressListener(ProgressListener progressListener) {}
				public void removeProgressListener(ProgressListener progressListener) {}
				private void reportProgress() {
					progress++;
					if (progress % 100 == 0) {
						progressListenerSupport.fireIncrementProgressEvent(100);
					}
				}
			};
	
			UUID trackUUID = builder.addTrack("gene", "Genes", featureSource);
			builder.setAttribute(trackUUID, "top", "0.42");
			builder.setAttribute(trackUUID, "height", "0.16");
			builder.setAttribute(trackUUID, "viewer", "Gene");
		}

		return builder.getDataset();
	}

	/**
	 * Convert a list of Chromosomes to a list of Sequences. Chromosomes is a simple object
	 * representing an entry in the UCSC chromInfo table, with a name and a length. We
	 * create a UUID for the sequence here and try (not very hard) to guess its topology.
	 */
	private List<Sequence> toSequences(List<Chromosome> chromosomes, Category category) {

		// TODO Guess topology more accurately
		// We assume circular topology for prokaryotes and linear otherwise.
		// This is wrong in many cases.
		Topology topology = category.isProkaryotic() ? Topology.circular : Topology.linear;

		List<Sequence> sequences = new ArrayList<Sequence>(chromosomes.size());
		for (Chromosome chr : chromosomes) {
			UUID uuid = UUID.randomUUID();
			BasicSequence seq = new BasicSequence(uuid, chr.getName(), chr.getSize(), topology);
			seq.getAttributes().put("ucsc.name", chr.getName());
			sequences.add(seq);
		}
		return sequences;
	}

	private int getExpectedProgress(String domain) {
		if ("archaea".equals(domain))
			return 10000;
		else if ("virus".equals(domain))
			return 5000;
		else if ("bacteria".equals(domain))
			return 20000;
		else if ("eukaryotes".equals(domain))
			return 100000;
		else
			return 50000;
	}
}
