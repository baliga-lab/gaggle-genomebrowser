package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.impl.BasicDataset;
import org.systemsbiology.genomebrowser.impl.BasicSequence;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.DatasetBuilder;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.Track;


// UG! The intention here was to separate the code for adding and editing elements
// of a dataset out of the dataset itself. It's quite possible that was an error
// in judgement. Maybe it would be better to have the dataset, sequences and
// tracks be able to mutate themselves.

// might it be cleaner to create the DB entries then load the dataset object
// from the db, rather than create the db entries and the dataset at the
// same time as I do here?

/**
 * Implementation of DatasetBuilder that relies on Sqlite. Note that the Dataset
 * constructed here is disconnected from the database. Updates to the Dataset
 * object are not automatically propogated back to the db.
 * @author cbare
 */
public class SqliteDatasetBuilder implements DatasetBuilder {
	private static final Logger log = Logger.getLogger(SqliteDatasetBuilder.class);
	private BasicDataset dataset;
	private SqliteDataSource sqlite;
	@SuppressWarnings("unused")
	private File dbFile;
	private SqliteTrackImporter sqliteTrackImporter;


	public SqliteDatasetBuilder(File dbFile) {
		sqlite = new SqliteDataSource(dbFile);
		sqliteTrackImporter = new SqliteTrackImporter(dbFile);
	}

	public UUID beginNewDataset(String name) {
		// TODO should call SqliteIo.newDataset or SqliteDataSource.newDataset
		UUID uuid = UUID.randomUUID();
		dataset = new BasicDataset(uuid, name);
		sqlite.createTablesAndWriteDatasetRecord(uuid, name);
		setAttribute(uuid, "created-on", new Date());
		setAttribute(uuid, "created-by", System.getProperty("user.name"));
		dataset.setSequenceFetcher(sqlite.loadSequenceFetcher());
		return uuid;
	}

	/**
	 * set attibute in DB and on the corresponding object.
	 */
	public void setAttribute(UUID uuid, String key, Object value) {
		sqlite.writeAttribute(uuid, key, value);
		// The following is a kinda sloppy process for finding the object with the
		// given uuid and applying the updates to its attributes. It might be a
		// good idea to create a HasAttributes interface?
		if (dataset.getUuid().equals(uuid))
			dataset.getAttributes().put(key, value);
		else {
			for (Sequence sequence : dataset.getSequences()) {
				if (sequence.getUuid().equals(uuid)) {
					sequence.getAttributes().put(key, value);
					return;
				}
			}
			for (Track<Feature> track : dataset.getTracks()) {
				if (track.getUuid().equals(uuid)) {
					track.getAttributes().put(key, value);
					return;
				}
			}
			log.warn("SqliteDatasetBuilder: set attribute on non-existant uuid: " + uuid);
		}
	}

	/**
	 * create a new sequence
	 * @return randomly generated UUID of new sequence
	 */
	public UUID addSequence(String seqId, int length, Topology topology) {
		UUID uuid = UUID.randomUUID();
		BasicSequence sequence = new BasicSequence(uuid, seqId, length, topology);
		log.info("sequence = " + sequence);
		dataset.addSequence(sequence);
		sqlite.writeSequence(dataset.getUuid(), uuid, seqId, length, topology);
		return uuid;
	}

	public void addSequences(List<Sequence> sequences) {
		for (Sequence sequence : sequences) {
			dataset.addSequence(sequence);
		}
		sqlite.writeSequences(dataset.getUuid(), sequences);
	}


	/**
	 * create a new track of the requested type containing features acquired
	 * from the given featureSource.
	 * @param trackType for example, "gene", "quantitative.positional", or "quantitative.segment"
	 * @param name Name of the new track
	 * @param featureSource
	 * @return the UUID of the new track
	 */
	public UUID addTrack(String trackType, String name, FeatureSource featureSource) {
		UUID uuid = null;
		if ("quantitative.segment".equals(trackType))
			uuid = sqliteTrackImporter.importQuantitativeSegmentTrack(name, dataset.getUuid(), featureSource);
		// oops, need number of columns
//		else if ("quantitative.segment.matrix".equals(trackType))
//			uuid = sqliteTrackImporter.importQuantitativeSegmentMatrixTrack(name, dataset.getUuid(), featureSource, columns);
		else if ("quantitative.positional".equals(trackType))
			uuid = sqliteTrackImporter.importQuantitativePositionalTrack(name, dataset.getUuid(), featureSource);
		else if ("gene".equals(trackType))
			uuid = sqliteTrackImporter.importGeneTrack(name, dataset.getUuid(), featureSource);
		else
			throw new RuntimeException("Unsupported track type: " + trackType);

		dataset.addTrack(sqlite.loadTrack(uuid));
		return uuid;
	}

	public Dataset getDataset() {
		return dataset;
	}
}
