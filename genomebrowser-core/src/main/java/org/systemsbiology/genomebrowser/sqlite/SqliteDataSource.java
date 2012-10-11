package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.util.Progress;
import org.systemsbiology.genomebrowser.bookmarks.Bookmark;
import org.systemsbiology.genomebrowser.bookmarks.BookmarkDataSource;
import org.systemsbiology.genomebrowser.bookmarks.ListBookmarkDataSource;
import org.systemsbiology.genomebrowser.util.CoordinateMapSelection;
import org.systemsbiology.genomebrowser.model.BasicDataset;
import org.systemsbiology.genomebrowser.model.BasicSequence;
import org.systemsbiology.genomebrowser.model.Block;
import org.systemsbiology.genomebrowser.model.FeatureBlock;
import org.systemsbiology.genomebrowser.model.GeneTrack;
import org.systemsbiology.genomebrowser.model.CoordinateMap;
import org.systemsbiology.genomebrowser.model.Coordinates;
import org.systemsbiology.genomebrowser.model.Dataset;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureFilter;
import org.systemsbiology.genomebrowser.model.GeneFeatureType;
import org.systemsbiology.genomebrowser.model.GeneFeatureImpl;
import org.systemsbiology.genomebrowser.model.NsafFeature;
import org.systemsbiology.genomebrowser.model.Range;
import org.systemsbiology.genomebrowser.model.Segment;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.genomebrowser.model.Strand;
import org.systemsbiology.genomebrowser.model.Topology;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.model.Feature.NamedFeature;
import org.systemsbiology.genomebrowser.util.Attributes;
import org.systemsbiology.util.FileUtils;
import org.systemsbiology.util.StringUtils;
import org.systemsbiology.genomebrowser.model.SequenceMapper;

// proposed changes for version 2 of schema:
// * add label column to attributes for use in switchable views
// * add min, max, and length columns to tracks table? max.value and min.value stashed in attributes for now.
// * add schema version stamp either to datasets table or in its own table



/**
 * Encapsulates interaction with the embedded DB. Loads blocks of track data,
 * generates the index of blocks for a track, etc.
 * @author cbare
 */
public class SqliteDataSource extends SqliteDb {
	private static final Logger log = Logger.getLogger(SqliteDataSource.class);
	private static final Pattern namePattern = Pattern.compile("(.*?)(?:_(\\d+))?");
	private String connectString;
	private Progress progress = new Progress();

	/**
	 * number of features in a block
	 */
	private int blockSize = 20000;

	// Note: the size of the cache is fixed to an arbitrary value, and also the
	// size of blocks is fixed to 20k features. Both of these could be tuned
	// based on the amount of available memory, size of features, etc.

	// use LinkedHashMap as an LRU-cache for blocks
	// seems like the cache shouldn't be owned by this class, since the cache
	// is a higher level design feature than choice of DB.
	@SuppressWarnings("serial")
	private LinkedHashMap<BlockKey, Block<? extends Feature>> cache = new LinkedHashMap<BlockKey, Block<? extends Feature>>(100, 0.75f, true) {
		private static final int MAX_ENTRIES = 100;
		protected boolean removeEldestEntry(Map.Entry<BlockKey, Block<? extends Feature>> eldest) {
	        return size() > MAX_ENTRIES;
	    }
	};


	public SqliteDataSource(File file) {
		this(getConnectStringForFile(file.getAbsolutePath()));
	}

	public SqliteDataSource(String connectString) {
		loadSqliteDriver();
		this.connectString = connectString;
	}

	public static String getConnectStringForFile(String filename) {
		return "jdbc:sqlite:" + filename;
	}

	static void loadSqliteDriver() {
		try {
			Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a fake empty database so we can get version info for the DB driver
	 */
	public static Map<String, String> getDatabaseInfo() {
		Connection conn = null;
		
		try {
			File tmp = File.createTempFile("test-sqlite-", ".db");
			tmp.deleteOnExit();

			loadSqliteDriver();
	        conn = DriverManager.getConnection("jdbc:sqlite:" + tmp.getAbsolutePath());
			DatabaseMetaData dbmd = conn.getMetaData();
			Map<String, String> props = new HashMap<String, String>();
			props.put("db.version", dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion());
			props.put("db.driver", dbmd.getDriverName() + " " + dbmd.getDriverVersion());
			return props;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	
	public static class DatasetDescription {
		public final UUID uuid;
		public final String name;

		public DatasetDescription(UUID uuid, String name) {
			this.name = name;
			this.uuid = uuid;
		}
	}

	/**
	 * Get a list of the available datasets.
	 * @return a List of DatasetDecriptions which have a uuid and a name.
	 */
	public List<DatasetDescription> getDatasets() {
		log.debug("getDatasets()");
		List<DatasetDescription> results = new ArrayList<DatasetDescription>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// check if there is a datasets table
			DatabaseMetaData dbmd = conn.getMetaData();
			rs = dbmd.getTables(null, null, "datasets", null);
			if (!rs.next()) return results;

			ps = conn.prepareStatement("select uuid, name from datasets;");
			rs = ps.executeQuery();
			while (rs.next()) {
				results.add(new DatasetDescription(UUID.fromString(rs.getString(1)), rs.getString(2)));
			}
			return results;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


	/**
	 * @return the first dataset in the DB, for cases where we can assume there's only one.
	 */
	public Dataset loadDataset() {
		List<DatasetDescription> list = getDatasets();
		if (list.size() < 1)
			throw new RuntimeException("No datasets found.");
		return loadDataset(list.get(0).uuid);
	}

	/**
	 * load the dataset with the given uuid, complete with its sequences
	 * and tracks.
	 * @param uuid UUID of the dataset
	 */
	public Dataset loadDataset(UUID uuid) {
		log.info("loading dataset " + uuid);
		progress.init(getTotalProgress(uuid));
		migrateDb();
		BasicDataset dataset = loadDatasetStub(uuid);
		dataset.getAttributes().putAll(getAttributes(uuid));
		progress.add(100);
		dataset.setSequences(getSequences(uuid));
		dataset.addTracks(loadTracks(uuid));
		dataset.setSequenceFetcher(loadSequenceFetcher());
		return dataset;
	}

	public void migrateDb() {
		// TODO create bookmarks tables
	}

	private BasicDataset loadDatasetStub(UUID uuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select name from datasets where uuid=?");
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			if (rs.next()) {
				BasicDataset dataset = new BasicDataset();
				dataset.setName(rs.getString(1));
				dataset.setUuid(uuid);
				return dataset;
			}
			return null;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * get sequences for a dataset with the given uuid.
	 * @param uuid UUID of the dataset
	 * @return a list of sequences in the dataset
	 */
	public List<Sequence> getSequences(UUID uuid) {
		log.info("loading sequences...");
		List<Sequence> sequences = getSequencesHelper(uuid);
		// takes forever to load large numbers of sequences because of this loop
		if (sequences.size() > 1000) {
			log.warn("There are " + sequences.size() + " sequences. To avoid a looooong delay, sequence attributes will NOT be loaded.");
			// Tag sequences so attributes can be loaded lazily, a feature not yet implemented
			// TODO implement lazy loading of sequence attributes?
			for (Sequence sequence: sequences) {
				sequence.getAttributes().put("__lazy__", true);
			}
		}
		else {
			for (Sequence sequence: sequences) {
				sequence.getAttributes().putAll(getAttributes(sequence.getUuid()));
				progress.add(100);
			}
		}
		log.info("loaded " + sequences.size() + " sequences");
		return sequences;
	}

	// cache sequences?
//	private Sequence getSequence(int id) {
//		Sequence sequence = sequences.get(id);
//		if (sequence==null) {
//			sequence = _getSequence(id);
//			sequences.put(id, sequence);
//		}
//		return sequence;
//	}

	public Progress getProgress() {
		return progress;
	}
	
	private Sequence getSequence(int id) {
		Sequence sequence = getSequenceStub(id);
		sequence.getAttributes().putAll(getAttributes(sequence.getUuid()));
		return sequence;
	}

	private Sequence getSequenceStub(int id) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select sequences.uuid, sequences.name, sequences.length, sequences.topology from sequences where id=?");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next()) {
				UUID sequenceUuid = UUID.fromString(rs.getString(1));
				String name = rs.getString(2);
				int length = rs.getInt(3);
				Topology topology = Topology.valueOf(rs.getString(4));
				Sequence sequence = new BasicSequence(sequenceUuid, name, length, topology);
				return sequence;
			}
			throw new RuntimeException("Sequence id \"" + id + "\" not found.");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Retrieve basic info for sequences in the dataset with the given uuid.
	 */
	private List<Sequence> getSequencesHelper(UUID uuid) {
		List<Sequence> sequences = new ArrayList<Sequence>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select sequences.uuid, sequences.name, sequences.length, sequences.topology from datasets_sequences join sequences on datasets_sequences.sequences_uuid=sequences.uuid where datasets_sequences.datasets_uuid=?");
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			while (rs.next()) {
				UUID sequenceUuid = UUID.fromString(rs.getString(1));
				String name = rs.getString(2);
				int length = rs.getInt(3);
				Topology topology = Topology.valueOf(rs.getString(4));
				sequences.add(new BasicSequence(sequenceUuid, name, length, topology));
				//log.debug("sequence= " + name);
				progress.add(20);
			}
			return sequences;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * get tracks for the dataset with the given uuid
	 * @param uuid UUID of the dataset
	 */
	public List<Track<? extends Feature>> loadTracks(UUID uuid) {
		List<TrackStub> stubs = loadTrackStubs(uuid);
		List<Track<? extends Feature>> tracks = new ArrayList<Track<? extends Feature>>();

		for (TrackStub stub: stubs) {
			tracks.add(loadTrack(stub));
			progress.add(100);
		}
		return tracks;
	}

	public Track<Feature> loadTrack(UUID trackUuid) {
		return loadTrack(loadTrackStub(trackUuid));
	}

	/**
	 * Given a stub, load a track. For block based tracks, load the block index instead
	 * of bringing all the features into memory. Gene tracks are small, so we just load
	 * features in.
	 * @return a subtype of Track<Feature>
	 */
	@SuppressWarnings("unchecked")
	public Track<Feature> loadTrack(TrackStub stub) {
		
		// The calls to getRange in here take a long time for large tracks.
		// We could add an index, but that seems like overkill. It would be
		// nicer to cache the range and length of tracks in the tracks table.
		// This brings up the need to record a schema version number somewhere
		// and have a procedure for migrating the schema when necessary.
		
		createBlockIndexTable();
		log.info("loading track: " + stub.name);
		Attributes attributes = getAttributes(stub.uuid);
		Track<? extends Feature> track = null;
		if ("gene".equals(stub.type)) {
			track = loadGeneTrack(stub);
		}
		else if ("peptide".equals(stub.type)) {
			track = new PeptideBlockTrack(stub.uuid, stub.name, getOrCreateBlockIndex(stub), this);
		}
		else if ("quantitative.segment".equals(stub.type)) {
			Range r = getRange(stub, attributes);
			track = new SegmentBlockTrack(stub.uuid, stub.name, getOrCreateBlockIndex(stub), r, this);
		}
		else if ("quantitative.positional".equals(stub.type)) {
			Range r = getRange(stub, attributes);
			track = new PositionalBlockTrack(stub.uuid, stub.name, getOrCreateBlockIndex(stub), r, this);
		}
		else if ("quantitative.positional.p.value".equals(stub.type)) {
			Range r = getRange(stub, attributes);
			track = new PositionalQuantitativePvalueBlockTrack(stub.uuid, stub.name, getOrCreateBlockIndex(stub), r, this);
		}
		else if ("quantitative.segment.matrix".equals(stub.type)) {
			Range r = getRange(stub, attributes);
			track = new SegmentMatrixBlockTrack(stub.uuid, stub.name, getOrCreateBlockIndex(stub), r, this);
		}
		// nsaf type for SSO fractionation/proteomics data
		else if ("nsaf".equals(stub.type)) {
			track = loadNsafTrack(stub);
		}
		else {
			throw new RuntimeException("Unknown track type: " + stub.type);
		}
		track.getAttributes().putAll(attributes);
		track.getAttributes().put("type", stub.type);

		return (Track<Feature>)track;
	}


	static class TrackStub {
		final UUID uuid;
		final String name;
		final String type;
		final String tableName;

		public TrackStub(UUID uuid, String name, String type, String tableName) {
			this.uuid = uuid;
			this.name = name;
			this.type = type;
			this.tableName = tableName;
		}
	}

	private static class SequenceKey {
		final int id;
		final String name;
		public SequenceKey(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private GeneTrack<GeneFeatureImpl> loadGeneTrack(TrackStub stub) {
		GeneTrack<GeneFeatureImpl> track = new GeneTrack<GeneFeatureImpl>(stub.uuid, stub.name);
		List<SequenceKey> keys = getSequenceKeys(stub);
		for (SequenceKey key: keys) {
			Sequence sequence = getSequence(key.id);
			for (Strand strand : Strand.both) {
				track.addGeneFeatures(new FeatureFilter(sequence, strand), 
						new FeatureBlock<GeneFeatureImpl>(sequence, strand, getGeneFeatureSubtrack(stub.tableName, key, strand)));
			}
		}
		return track;
	}

	/**
	 * retrieve genes on a given sequence and strand
	 * @param table
	 * @param key
	 * @param strand
	 * @return
	 */
	private List<GeneFeatureImpl> getGeneFeatureSubtrack(String table, SequenceKey key, Strand strand) {
		int count = getFeatureCount(table, key.id);
		List<GeneFeatureImpl> features = new ArrayList<GeneFeatureImpl>(count);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("select start, end, name, common_name, gene_type from " + table + "  where sequences_id=? and strand=? order by start, end, strand;");
			ps.setInt(1, key.id);
			ps.setString(2, strand.toAbbreviatedString());
			rs = ps.executeQuery();
			while (rs.next()) {
				int start = rs.getInt(1);
				int end = rs.getInt(2);
				String name = rs.getString(3);
				String commonName = rs.getString(4);
				String geneType = rs.getString(5);
				features.add(new GeneFeatureImpl(key.name, strand, Math.min(start, end), Math.max(start, end), name, commonName, GeneFeatureType.fromString(geneType, GeneFeatureType.other)));
			}
//			progress.add(features.size());
			return features;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private GeneTrack<NsafFeature> loadNsafTrack(TrackStub stub) {
		GeneTrack<NsafFeature> track = new GeneTrack<NsafFeature>(stub.uuid, stub.name);
		List<SequenceKey> keys = getSequenceKeys(stub);
		for (SequenceKey key: keys) {
			Sequence sequence = getSequence(key.id);
			for (Strand strand : Strand.both) {
				track.addGeneFeatures(new FeatureFilter(sequence, strand), 
						new FeatureBlock<NsafFeature>(sequence, strand, getNsafFeatureSubtrack(stub.tableName, key, strand)));
			}
		}
		track.setFeatureClass(NsafFeature.class);
		return track;
	}

	/**
	 * retrieve nsaf features on a given sequence and strand
	 */
	private List<NsafFeature> getNsafFeatureSubtrack(String table, SequenceKey key, Strand strand) {
		int count = getFeatureCount(table, key.id);
		List<NsafFeature> features = new ArrayList<NsafFeature>(count);
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		/*
		  sequences_id integer, strand text, start integer, end integer, gene_type text,
		  NSAF_in_Media_Secretion2_April_24_2009 numeric,
		  NSAF_in_SEC_Secretion1_April_16_2009 numeric,
		  NSAF_in_SEC_Secretion2_April_24_2009 numeric,
		  NSAF_in_SEC_Secretion2_Mar_10_2009 numeric,
		  NSAF_in_SEC_Secretome_July_13_2009 numeric,
		  NSAF_in_s_MEM_SLayer_July_9_2009 numeric,
		  name text,
		  common_name text,
		  degenerate integer
		 */

		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("select start, end, name, common_name, gene_type, " +
					" NSAF_in_Media_Secretion2_April_24_2009, " +
					" NSAF_in_SEC_Secretion1_April_16_2009, " +
					" NSAF_in_SEC_Secretion2_April_24_2009, " +
					" NSAF_in_SEC_Secretion2_Mar_10_2009, " +
					" NSAF_in_SEC_Secretome_July_13_2009, " +
					" NSAF_in_s_MEM_SLayer_July_9_2009, " +
					" degenerate " + 
					" from " + table + "  where sequences_id=? and strand=? order by start, end, strand;");
			ps.setInt(1, key.id);
			ps.setString(2, strand.toAbbreviatedString());
			rs = ps.executeQuery();
			while (rs.next()) {
				int start = rs.getInt(1);
				int end = rs.getInt(2);
				String name = rs.getString(3);
				String commonName = rs.getString(4);
				String geneType = rs.getString(5);
				float[] nsaf = new float[6];
				for (int i=0; i<6; i++) {
					nsaf[i] = rs.getFloat(i+6);
				}
				boolean degenerate = rs.getBoolean(12);
				features.add(new NsafFeature(key.name, strand, Math.min(start, end), Math.max(start, end),
						     name, commonName, GeneFeatureType.fromString(geneType, GeneFeatureType.other),
						     nsaf, degenerate));
			}
//			progress.add(features.size());
			return features;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


//	private GeneTrack<ScoredNamedFeature> OLDloadPeptideTrack(TrackStub stub) {
//		GeneTrack<ScoredNamedFeature> track = new GeneTrack<ScoredNamedFeature>(stub.uuid, stub.name);
//		track.setFeatureClass(ScoredNamedFeature.class);
//		List<SequenceKey> keys = getSequenceKeys(stub);
//		for (SequenceKey key: keys) {
//			Sequence sequence = getSequence(key.id);
//			for (Strand strand : Strand.both) {
//				track.addGeneFeatures(new FeatureFilter(sequence, strand), 
//						new FeatureBlock<ScoredNamedFeature>(sequence, strand, getPeptideFeatureSubtrack(stub.tableName, key, strand)));
//			}
//		}
//		return track;
//	}
//
//	/**
//	 * retrieve genes on a given sequence and strand
//	 * @param table
//	 * @param key
//	 * @param strand
//	 * @return
//	 */
//	private List<ScoredNamedFeature> getPeptideFeatureSubtrack(String table, SequenceKey key, Strand strand) {
//		int count = getFeatureCount(table, key.id);
//		List<ScoredNamedFeature> features = new ArrayList<ScoredNamedFeature>(count);
//		Connection conn = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//
//		try {
//			conn = DriverManager.getConnection(connectString);
//			ps = conn.prepareStatement("select start, end, name, common_name, gene_type, score from " + table + "  where sequences_id=? and strand=? order by start, end, strand;");
//			ps.setInt(1, key.id);
//			ps.setString(2, strand.toAbbreviatedString());
//			rs = ps.executeQuery();
//			while (rs.next()) {
//				int start = rs.getInt(1);
//				int end = rs.getInt(2);
//				String name = rs.getString(3);
//				String commonName = rs.getString(4);
//				String geneType = rs.getString(5);
//				double score = rs.getDouble(6);
//				features.add(new ScoredNamedFeature(key.name, strand, Math.min(start, end), Math.max(start, end), name, commonName, GeneFeatureType.fromString(geneType, GeneFeatureType.other), score));
//			}
////			progress.add(features.size());
//			return features;
//		}
//		catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//		finally {
//			try {
//				if (rs != null)
//					rs.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing result set", e1);
//			}
//			try {
//				if (ps != null)
//					ps.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing prepared statement", e1);
//			}
//			try {
//				if (conn != null)
//					conn.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing connection", e1);
//			}
//		}
//	}


	/**
	 * Get basic information, but not features, for tracks belonging to a dataset
	 * @param datasetUuid dataset uuid
	 * @return a list of TrackStubs for tracks belonging to the dataset with the given uuid
	 */
	List<TrackStub> loadTrackStubs(UUID datasetUuid) {
		List<TrackStub> stubs = new ArrayList<TrackStub>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select tracks.uuid, tracks.name, tracks.type, tracks.table_name from datasets_tracks join tracks on datasets_tracks.tracks_uuid=tracks.uuid where datasets_tracks.datasets_uuid=?");
			ps.setString(1, datasetUuid.toString());
			rs = ps.executeQuery();
			while (rs.next()) {
				TrackStub stub = new TrackStub(
					UUID.fromString(rs.getString(1)),
					rs.getString(2),
					rs.getString(3),
					rs.getString(4));
				stubs.add(stub);
			}
			return stubs;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Get track stub from its UUID 
	 * @param uuid UUID of track
	 */
	TrackStub loadTrackStub(UUID uuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select uuid, name, type, table_name from tracks where uuid=?");
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			if (rs.next()) {
				TrackStub stub = new TrackStub(
						UUID.fromString(rs.getString(1)),
						rs.getString(2),
						rs.getString(3),
						rs.getString(4));
				return stub;
			}
			throw new RuntimeException("Track " + uuid + " not found.");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Get a TrackStub for the track with the given name
	 * @throws RuntimeException if no track with the given name exists
	 */
	TrackStub loadTrackStub(String name) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select uuid, name, type, table_name from tracks where name=?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next()) {
				TrackStub stub = new TrackStub(
						UUID.fromString(rs.getString(1)),
						rs.getString(2),
						rs.getString(3),
						rs.getString(4));
				return stub;
			}
			throw new RuntimeException("Track " + StringUtils.quote(name) + " not found.");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Creates a SQLite backed implementation of SequenceFetcher if the
	 * bases table exists, returns null otherwise
	 */
	public SequenceFetcher loadSequenceFetcher() {
		return new SequenceDAO(connectString, this);
	}

	/**
	 * Get attributes for the object (dataset, sequence, or track)
	 * with the given uuid.
	 */
	public Attributes getAttributes(UUID uuid) {
		Attributes attr = new Attributes();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			ps = conn.prepareStatement("select key, value from attributes where uuid like ?");
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			while (rs.next()) {
				attr.put(rs.getString(1), rs.getObject(2));
			}
			return attr;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public SegmentBlock loadSegmentBlock(BlockKey key) {
		Block<? extends Feature> block = cache.get(key);
		if (block==null) {
			//log.info("miss");
			block = _loadSegmentBlock(key);
			cache.put(key, block);
		}
		return (SegmentBlock)block;
	}

	public SegmentBlock _loadSegmentBlock(BlockKey key) {
		int[] starts = new int[key.getLength()];
		int[] ends = new int[key.getLength()];
		double[] values = new double[key.getLength()];
		int i=0;

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("Select start, end, value from " + key.getTable() + " where rowId>=? and rowId<=?;");
			ps.setInt(1, key.getFirstRowId());
			ps.setInt(2, key.getLastRowId());
			rs = ps.executeQuery();
			while (rs.next()) {
				starts[i] = rs.getInt(1);
				ends[i] = rs.getInt(2);
				values[i] = rs.getDouble(3);
				i++;
			}

			return new SegmentBlock(key, starts, ends, values);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public PositionalBlock loadPositionalBlock(BlockKey key) {
		Block<? extends Feature> block = cache.get(key);
		if (block==null) {
			//log.info("miss");
			block = _loadPositionalBlock(key);
			cache.put(key, block);
		}
		return (PositionalBlock)block;
	}

	public PositionalBlock _loadPositionalBlock(BlockKey key) {
		int[] positions = new int[key.getLength()];
		double[] values = new double[key.getLength()];
		int i=0;

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("Select position, value from " + key.getTable() + " where rowId>=? and rowId<=?;");
			ps.setInt(1, key.getFirstRowId());
			ps.setInt(2, key.getLastRowId());
			rs = ps.executeQuery();
			while (rs.next()) {
				positions[i] = rs.getInt(1);
				values[i] = rs.getDouble(2);
				i++;
			}

			return new PositionalBlock(key, positions, values);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public PositionalQuantitativePvalueBlock loadPositionalQuantitativePvalueBlock(BlockKey key) {
		Block<? extends Feature> block = cache.get(key);
		if (block==null) {
			//log.info("miss");
			block = _loadPositionalQuantitativePvalueBlock(key);
			cache.put(key, block);
		}
		return (PositionalQuantitativePvalueBlock)block;
	}

	public PositionalQuantitativePvalueBlock _loadPositionalQuantitativePvalueBlock(BlockKey key) {
		int[] positions = new int[key.getLength()];
		double[] values = new double[key.getLength()];
		double[] pvalues = new double[key.getLength()];
		int i=0;

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("Select position, value, p_value from " + key.getTable() + " where rowId>=? and rowId<=?;");
			ps.setInt(1, key.getFirstRowId());
			ps.setInt(2, key.getLastRowId());
			rs = ps.executeQuery();
			while (rs.next()) {
				positions[i] = rs.getInt(1);
				values[i] = rs.getDouble(2);
				pvalues[i] = rs.getDouble(3);
				i++;
			}

			return new PositionalQuantitativePvalueBlock(key, positions, values, pvalues);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public SegmentMatrixBlock loadSegmentMatrixBlock(BlockKey key) {
		Block<? extends Feature> block = cache.get(key);
		if (block==null) {
			//log.info("miss");
			block = _loadSegmentMatrixBlock(key);
			cache.put(key, block);
		}
		return (SegmentMatrixBlock)block;
	}

	public SegmentMatrixBlock _loadSegmentMatrixBlock(BlockKey key) {
		int i=0;
		int[] starts = new int[key.getLength()];
		int[] ends = new int[key.getLength()];

		// TODO get this once and keep it somewhere
		List<String> columns = getMatrixColumns(key.getTable());
		int cols = columns.size();

		double[][] values = new double[key.getLength()][cols];

		StringBuilder sql = new StringBuilder("Select start, end");
		for (String column : columns) {
			sql.append(", ").append(column);
		}
		sql.append(" from ").append(key.getTable()).append(" where rowId>=? and rowId<=?;");

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement(sql.toString());
			ps.setInt(1, key.getFirstRowId());
			ps.setInt(2, key.getLastRowId());
			rs = ps.executeQuery();
			while (rs.next()) {
				starts[i] = rs.getInt(1);
				ends[i] = rs.getInt(2);

				for (int j=0; j<cols; j++)
					values[i][j] = rs.getDouble(3+j);

				i++;
			}
			
			if (i != key.getLength())
				log.warn("wrong number of features in block: " + key);

			return new SegmentMatrixBlock(key, starts, ends, values);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public PeptideBlock loadPeptideBlock(BlockKey key) {
		Block<? extends Feature> block = cache.get(key);
		if (block==null) {
			//log.info("miss");
			block = _loadPeptideBlock(key);
			cache.put(key, block);
		}
		return (PeptideBlock)block;
	}

	public PeptideBlock _loadPeptideBlock(BlockKey key) {
		int[] starts = new int[key.getLength()];
		int[] ends = new int[key.getLength()];
		String[] names = new String[key.getLength()];
		String[] commonNames = new String[key.getLength()];
		double[] scores = new double[key.getLength()];
		int[] redundancy = new int[key.getLength()];
		int i=0;

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			boolean hasRedundancy = tableHasColumn(conn, key.getTable(), "redundancy");
			String sql = hasRedundancy ? 
					"Select start, end, name, common_name, score, redundancy from " + key.getTable() + " where rowId>=? and rowId<=?;" :
					"Select start, end, name, common_name, score from " + key.getTable() + " where rowId>=? and rowId<=?;";
			
			ps = conn.prepareStatement(sql);
			ps.setInt(1, key.getFirstRowId());
			ps.setInt(2, key.getLastRowId());
			rs = ps.executeQuery();
			while (rs.next()) {
				starts[i] = rs.getInt(1);
				ends[i] = rs.getInt(2);
				names[i] = rs.getString(3);
				commonNames[i] = rs.getString(4);
				scores[i] = rs.getDouble(5);
				if (hasRedundancy) redundancy[i] = rs.getInt(6);
				i++;
			}

			return hasRedundancy ?
				new PeptideBlock(key, starts, ends, names, commonNames, scores, redundancy) :
				new PeptideBlock(key, starts, ends, names, commonNames, scores);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public BlockIndex getOrCreateBlockIndex(TrackStub stub) {
		BlockIndex index = loadBlockIndex(stub);
		if (index.size() == 0) {
			index = createBlockIndex(stub);
			saveBlockIndex(index);
		}
		return index;
	}

	public BlockIndex loadBlockIndex(TrackStub stub) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		BlockIndex index = new BlockIndex();

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement(
					"select sequences_id, seqId, strand, start, end, length, first_row_id, last_row_id " +
					"from block_index " +
					"where tracks_uuid=?" +
					"order by sequences_id, strand, start, end;");
			ps.setString(1, stub.uuid.toString());

			rs = ps.executeQuery();
			while (rs.next()) {
				BlockKey blockKey = new BlockKey(
						stub.uuid,
						rs.getInt(1),
						rs.getString(2),
						Strand.fromString(rs.getString(3)),
						rs.getInt(4),
						rs.getInt(5),
						rs.getInt(6),
						stub.tableName,
						rs.getInt(7),
						rs.getInt(8));
				index.add(blockKey);
//				progress.add(blockKey.getFeatureCount());
			}
			log.info("loadBlockIndex for track: " + stub.name + ", " + index.size() + " blocks");
			return index;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Given a track's uuid, create a BlockIndex for that track. Using a
	 * BlockIndex, you can get all blocks that lie within a given window
	 * on the genome.
	 * 
	 * @see org.systemsbiology.genomebrowser.sqlite.BlockIndex
	 */
	BlockIndex createBlockIndex(TrackStub stub) {
		log.info("create block index for track " + stub.tableName);
		BlockIndex index = new BlockIndex();

		List<SequenceKey> keys = getSequenceKeys(stub);

		for (SequenceKey seqKey : keys) {
			for (Strand strand : getStrandsForTrack(stub)) {

				// count the features in the track on a given sequence and strand
				int count = getFeatureCount(stub.tableName, seqKey.id, strand);

				// compute the number of blocks needed to hold the features
				int blockCount = (int)Math.ceil( ((double)count) / blockSize );

				//System.out.format("sequence=%s, strand=%s, blockCount=%d\n", sequences.get(sequencesId), strand.toAbbreviatedString(), blockCount);
				int firstRowId0 = getFirstRow(stub.tableName, seqKey.id, strand);
				int lastRowId0 = firstRowId0 + count - 1;

				for (int i=0; i<blockCount; i++) {
					int firstRowId = i * blockSize + firstRowId0;
					int lastRowId = Math.min(lastRowId0, firstRowId + blockSize - 1);
					BlockDimensions dim = getBlockDimensions(stub, firstRowId, lastRowId);
					if (dim.length<=0)
						log.warn("block length = " + dim.length + " for track " + stub.name);
//					progress.add(dim.length);

					index.add(new BlockKey(
							stub.uuid,
							seqKey.id,
							seqKey.name,
							strand,
							dim.start,
							dim.end,
							dim.length,
							stub.tableName,
							firstRowId,
							lastRowId));
				}
			}
		}
		log.info("create block index for track: " + stub.name + ", " + index.size() + " blocks");
		return index;
	}
	
	
	/*
	 * TODO: make block index hierarchical
	 * 
	 * The block index divides a large track into contiguous blocks for
	 * caching purposes. A block is a lot like a chunky feature, with a
	 * sequence, strand, start, and end of its own. After recognizing that, it
	 * occurred to me that blocks and features should be an application of the
	 * Composite pattern. A block could also have a value, or set of values
	 * like (mean, median, min, max). This would help with zooming out to
	 * resolutions where many primary data points overlap each pixel. Also,
	 * if blocks could be composed of other blocks, that would give a truly
	 * multiscale view of the data.
	 *
	 * Blocks could also be used to store data more efficiently. If we already
	 * have the information that rowID=0 through 20000 is on sequence 1 and
	 * strand +, why repeat that in the feature table? Could also be used to
	 * store whether the positions or start,end coordinates are regular and if
	 * so what are they? Every position? Probes of 60 bp, placed every 20 bp?
	 * 
	 * Maybe even resampling the data from irregular to regular would be
	 * worthwhile?
	 * 
	 */

	private void createBlockIndexTable() {

		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement(
					"create table if not exists block_index (" +
					"tracks_uuid text not null," +
					"sequences_id integer not null," +
					"seqId text not null," +
					"strand text not null," +
					"start integer not null," +
					"end integer not null," +
					"length integer not null," +
					"table_name text not null," +
					"first_row_id integer not null," +
					"last_row_id integer not null);");
			ps.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private void saveBlockIndex(BlockIndex blockIndex) {
		for (BlockKey key: blockIndex.keys()) {
			saveBlockIndexEntry(key);
		}
	}

	private void saveBlockIndexEntry(BlockKey key) {

		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(connectString);

//			private final UUID trackUuid;
//			private final String seqId;
//			private final Strand strand;
//			private final int start;
//			private final int end;
//			private final int length;
//			private final String table;
//			private final int firstRowId;
//			private final int lastRowId;

			ps = conn.prepareStatement("insert into block_index values (?,?,?,?,?,?,?,?,?,?);");
			ps.setString(1, key.getTrackUuid().toString());
			ps.setInt(2, key.getSequencesId());
			ps.setString(3, key.getSeqId());
			ps.setString(4, key.getStrand().toAbbreviatedString());
			ps.setInt(5, key.getStart());
			ps.setInt(6, key.getEnd());
			ps.setInt(7, key.getLength());
			ps.setString(8, key.getTable());
			ps.setInt(9, key.getFirstRowId());
			ps.setInt(10, key.getLastRowId());

			ps.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Get the range of data values in a numeric track.
	 * @param uuid UUID of the track
	 * @return range of values
	 */
	public Range getMatrixRange(TrackStub stub) {
		List<String> columns = getMatrixColumns(stub.tableName);
		List<Range> ranges = new ArrayList<Range>(columns.size());
		for (String column : columns) {
			ranges.add(getRange(stub, column));
		}
		return Range.consolidate(ranges);
	}

	/**
	 * Any column that matches "value%" is a value column. Return a
	 * list of such columns in the given table.
	 */
	private List<String> getMatrixColumns(String table) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			DatabaseMetaData dbmd = conn.getMetaData();
			rs = dbmd.getColumns(null, null, table, "value%");
			List<String> columns = new ArrayList<String>();
			while (rs.next()) {
				columns.add(rs.getString("COLUMN_NAME"));
			}
			return columns;

		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}

	}

	/**
	 * Get the range of data values in a numeric track.
	 * @param uuid UUID of the track
	 * @return range of values
	 */
//	public Range getRange(UUID uuid) {
//		String table = getTableForTrack(uuid);
//
//		Connection conn = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//
//		try {
//			conn = DriverManager.getConnection(connectString);
//
//			ps = conn.prepareStatement("select min(value), max(value) from " + table + ";");
//			rs = ps.executeQuery();
//			if (rs.next()) {
//				return new Range(rs.getDouble(1), rs.getDouble(2));
//			}
//			return new Range(0,0);
//		}
//		catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//		finally {
//			try {
//				if (rs != null)
//					rs.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing result set", e1);
//			}
//			try {
//				if (ps != null)
//					ps.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing prepared statement", e1);
//			}
//			try {
//				if (conn != null)
//					conn.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing connection", e1);
//			}
//		}
//	}

	/**
	 * Get range of a track, first by looking in attributes for
	 * min.value and max.value. Failing to find those, we find the min
	 * and max value by querying, which can be slow for large tables.
	 */
	public Range getRange(TrackStub stub, Attributes attributes) {
		double min = attributes.getDouble("min.value", Double.NaN);
		double max = attributes.getDouble("max.value", Double.NaN);
		Range range;
		if (Double.isNaN(min) || Double.isNaN(max)) {
			if ("quantitative.segment.matrix".equals(stub.type)) {
				range = getMatrixRange(stub);
			}
			else {
				range = getRange(stub, "value");
			}
			writeAttribute(stub.uuid, "min.value", range.min);
			writeAttribute(stub.uuid, "max.value", range.max);
		}
		else {
			range = new Range(min, max);
		}
		return range;
	}

	/**
	 * Get the range of data values in a numeric track.
	 * @param uuid UUID of the track
	 * @return range of values
	 */
	private Range getRange(TrackStub stub, String column) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select min(" + column + "), max(" + column + ") from " + stub.tableName + ";");
			rs = ps.executeQuery();
			if (rs.next()) {
				return new Range(rs.getDouble(1), rs.getDouble(2));
			}
			return new Range(0,0);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

//	private String getTableForTrack(UUID uuid) {
//		Connection conn = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//
//		try {
//			conn = DriverManager.getConnection(connectString);
//
//			// get length to alloc arrays
//			ps = conn.prepareStatement("select table_name from tracks where uuid=?;");
//			ps.setString(1, uuid.toString());
//			rs = ps.executeQuery();
//			if (rs.next()) {
//				return rs.getString(1);
//			}
//			throw new RuntimeException("Entry not found for track: " + uuid.toString());
//		}
//		catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//		finally {
//			try {
//				if (rs != null)
//					rs.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing result set", e1);
//			}
//			try {
//				if (ps != null)
//					ps.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing prepared statement", e1);
//			}
//			try {
//				if (conn != null)
//					conn.close();
//			}
//			catch (Exception e1) {
//				log.warn("Error closing connection", e1);
//			}
//		}
//	}

	// TODO fix exception
	// reloading dataset: /Users/cbare/Documents/hbgb/halo_tiling_demo.hbgb
	// apparently saw stale values in loadTrackStubs(uuid);
	//
	// java.lang.RuntimeException: java.sql.SQLException: no such table: features_from_r_1
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.getSequenceKeys(SqliteDataSource.java:1478)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.createBlockIndex(SqliteDataSource.java:1080)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.getOrCreateBlockIndex(SqliteDataSource.java:1000)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.loadTrack(SqliteDataSource.java:479)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.loadTracks(SqliteDataSource.java:437)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.loadDataset(SqliteDataSource.java:223)
	//   at org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.loadDataset(SqliteDataSource.java:207)
	// caused by: java.sql.SQLException: no such table: features_from_r_1

	/**
	 * Get name and id for sequences referenced in the given track.
	 */
	private List<SequenceKey> getSequenceKeys(TrackStub stub) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select id, name from sequences where id in (select distinct sequences_id from " + stub.tableName + ");");
			rs = ps.executeQuery();

			List<SequenceKey> keys = new ArrayList<SequenceKey>();
			while (rs.next()) {
				int id = rs.getInt(1);
				String name = rs.getString(2);
				keys.add(new SequenceKey(id, name));
			}
			return keys;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}
	
	public int getFeatureCount(String table, int sequencesId, Strand strand) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select count(*) from " + table + " where sequences_id=? and strand=?;");
			ps.setInt(1, sequencesId);
			ps.setString(2, strand.toAbbreviatedString());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public int getFeatureCount(String table, int sequencesId) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select count(*) from " + table + " where sequences_id=?;");
			ps.setInt(1, sequencesId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public int getFeatureCount(String table) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select count(*) from " + table + ";");
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private int countSequences(UUID datasetUuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select count(*) from datasets_sequences where datasets_uuid like ?;");
			ps.setString(1, datasetUuid.toString());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			return 0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * 100 for each track plus 100 for each sequence plus 100 for each track
	 * @param datasetUuid
	 * @return
	 */
	private int getTotalProgress(UUID datasetUuid) {
		int total = 0;
		// 100 for the dataset itself and 100 for each sequence
		total += 100 + 100 * countSequences(datasetUuid);
		// loadTrackStubs is redundant, but goes quickly.
		List<TrackStub> stubs = loadTrackStubs(datasetUuid);
		total += 100 * stubs.size();

		// counting features can take a while
//		for (TrackStub stub: stubs) {
//			total += getFeatureCount(stub.tableName);
//		}
		return total;
	}

	private List<Strand> getStrandsForTrack(TrackStub stub) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select distinct strand from " + stub.tableName + ";");
			rs = ps.executeQuery();
			List<Strand> strands = new ArrayList<Strand>();
			while (rs.next()) {
				strands.add(Strand.fromString(rs.getString(1)));
			}
			return strands;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private int getFirstRow(String table, int sequencesId, Strand strand) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			// get length to alloc arrays
			ps = conn.prepareStatement("select min(rowId) from " + table + " where sequences_id=? and strand=?;");
			ps.setInt(1, sequencesId);
			ps.setString(2, strand.toAbbreviatedString());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			throw new RuntimeException(String.format("First row not found for: table=%s, seq=%d, strand=%s", table, sequencesId, strand.toAbbreviatedString()));
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private BlockDimensions getBlockDimensions(TrackStub stub, int firstRowId, int lastRowId) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			
			//log.info("getting block dimensions: " + stub.name + " -- " + firstRowId + ", " + lastRowId);

			// get length to alloc arrays
			if (stub.type.toLowerCase().indexOf("positional") > -1) {
				ps = conn.prepareStatement(
						"select min(position), max(position), count(*)" +
						" from " + stub.tableName +
						" where rowId>=? and rowId<=?;");
			}
			else {
				ps = conn.prepareStatement(
						"select min(start), max(end), count(*)" +
						" from " + stub.tableName +
						" where rowId>=? and rowId<=?;");
			}
			ps.setInt(1, firstRowId);
			ps.setInt(2, lastRowId);
			rs = ps.executeQuery();
			if (rs.next()) {
				// BlockDimensions(start, end, length)
				return new BlockDimensions(
						rs.getInt(1),
						rs.getInt(2),
						rs.getInt(3));
			}
			throw new RuntimeException(String.format("Dimensions not found for: table=%s, rows=%d, %d", stub.tableName, firstRowId, lastRowId));
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private static class BlockDimensions {
		int start;
		int end;
		int length;

		public BlockDimensions(int start, int end, int length) {
			this.start = start;
			this.end = end;
			this.length = length;
		}
	}


	public int countRows(String table) {
		Connection conn = null;
		Statement s = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			s = conn.createStatement();
			rs = s.executeQuery("select count(*) from " + table + ";");
			if (rs.next()) {
				return rs.getInt(1);
			}
			return -1;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public boolean deleteTrack(UUID uuid) {
		try {
			return deleteTrack(loadTrackStub(uuid));
		}
		catch (Exception e) {
			return false;
		}
	}

	public boolean deleteTrackByName(String name) {
		try {
			return deleteTrack(loadTrackStub(name));
		}
		catch (Exception e) {
			return false;
		}
	}
	
	// clean block_index: delete from block_index where tracks_uuid not in (select uuid from tracks);

	public boolean deleteTrack(TrackStub stub) {
		Connection conn = null;
		Statement s = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			beginTransaction(conn);

			s = conn.createStatement();
			try {
				s.executeUpdate("drop table if exists " + stub.tableName + ";");
			}
			catch (SQLException e) {
				if (e.getMessage().contains("use DROP VIEW to delete view")) {
					s.executeUpdate("drop view if exists " + stub.tableName + ";");
				}
				else {
					throw e;
				}
			}
			s.close();

			// delete entry from datasets_tracks
			ps = conn.prepareStatement("delete from datasets_tracks where tracks_uuid=?");
			ps.setString(1, stub.uuid.toString());
			ps.executeUpdate();

			// delete entries from block_index
			ps = conn.prepareStatement("delete from block_index where tracks_uuid=?");
			ps.setString(1, stub.uuid.toString());
			ps.executeUpdate();

			// delete attributes
			ps = conn.prepareStatement("delete from attributes where uuid=?");
			ps.setString(1, stub.uuid.toString());
			ps.executeUpdate();

			// delete entry from track table
			ps = conn.prepareStatement("delete from tracks where uuid=?");
			ps.setString(1, stub.uuid.toString());
			int count = ps.executeUpdate();
			
			commitTransaction(conn);
			
			return count > 0;
		}
		catch (SQLException e) {
			log.warn(e);
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


	/**
	 * For each sequence where the given track has features, return
	 * the range of coordinates covered by data in the given track.
	 * @return a Map from the name of the sequence to the range of feature data 
	 */
	public List<Segment> getTrackCoordinateRange(UUID trackUuid) {
		TrackStub stub = loadTrackStub(trackUuid);

		List<Segment> segments = new ArrayList<Segment>(); 
		String sql = null;
		if ("quantitative.segment".equals(stub.type)) {
			sql = "select s.name, min(t.start), max(t.end) from " + stub.tableName + " t join sequences s on t.sequences_id=s.id group by s.id;";
		}
		else if ("quantitative.positional".equals(stub.type)) {
			sql = "select s.name, min(t.position), max(t.position) from " + stub.tableName + " t join sequences s on t.sequences_id=s.id group by s.id;";
		}
		else {
			log.warn("can't get track coordinate range for track:" + stub);
			return segments;
		}

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next()) {
				segments.add(new Segment(rs.getString(1), rs.getInt(2), rs.getInt(3)));
			}
			return segments;
		}
		catch (SQLException e) {
			log.warn(e);
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing resultset", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Creates a new dataset using the current connect string
	 * with a the given name and a randomly created UUID. We'll
	 * need to add sequences before we can do anything else.
	 * @param name dataset name
	 */
	public Dataset newDataset(String name) {
		log.info(String.format("Creating new dataset \"%s\" in sqlite db \"%s\"", name, connectString));
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			createTables(conn);
			UUID uuid = UUID.randomUUID();

			beginTransaction(conn);
			writeDatasetRecord(conn, uuid, name);

			// create empty dataset.
			// We'll need to add sequences before we can do anything.
			BasicDataset dataset = new BasicDataset();
			dataset.setName(name);
			dataset.setUuid(uuid);
			dataset.getAttributes().put("created-on", new Date());
			dataset.getAttributes().put("created-by", System.getProperty("user.name"));

			writeAttributes(conn, uuid, dataset.getAttributes());

			commitTransaction(conn);
			return dataset;
		}
		catch (SQLException e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (Exception e1) {
					log.error(e1);
				}
			}
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public void createTablesAndWriteDatasetRecord(UUID uuid, String name) {
		log.info(String.format("Creating new dataset \"%s\" in sqlite db \"%s\"", name, connectString));
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			createTables(conn);
			writeDatasetRecord(conn, uuid, name);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	
	private void writeDatasetRecord(Connection conn, UUID uuid, String name) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("insert into datasets values(?, ?);");
			ps.setString(1, uuid.toString());
			ps.setString(2, name);
			ps.execute();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
		}
	}

	public void writeAttribute(UUID uuid, String key, Object value) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			writeAttribute(conn, uuid, key, value);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public void writeAttribute(Connection conn, UUID uuid, String key, Object value) {
		PreparedStatement ps = null;
		try {
			// remove existing attributes
			ps = conn.prepareStatement("delete from attributes where uuid=? and key=?");
			ps.setString(1, uuid.toString());
			ps.setString(2, key);
			ps.execute();
			ps.close();

			ps = conn.prepareStatement("insert into attributes values(?,?,?)");
			ps.setString(1, uuid.toString());
			ps.setString(2, key);
			ps.setObject(3, value);
			ps.execute();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (Exception e) {
					log.error("Exception while closing statement", e);
				}
			}
		}
	}

	public void writeAttributes(UUID uuid, Attributes attributes) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			writeAttributes(conn, uuid, attributes);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public void writeAttributes(Connection conn, UUID uuid, Attributes attributes) {
		PreparedStatement ps = null;

		try {
			// remove existing attributes
			ps = conn.prepareStatement("delete from attributes where uuid=?");
			ps.setString(1, uuid.toString());
			ps.execute();
			ps.close();

			ps = conn.prepareStatement("insert into attributes values(?,?,?)");

			for (String key: attributes.keySet()) {
				ps.setString(1, uuid.toString());
				ps.setString(2, key);
				ps.setObject(3, attributes.get(key));
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e) {
			throw new RuntimeException("Error writing attributes to Sqlite DB", e);
		}
		finally {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (Exception e) {
					log.error("Exception while closing statement", e);
				}
			}
		}
	}

	// CREATE TABLE sequences (
	//     id integer primary key AUTOINCREMENT not null,
	//     uuid text not null,
	//     name text not null,
	//     length integer not null,
	//     topology text);

	public void writeSequence(UUID datasetUuid, UUID seqUuid, String name, int length, Topology topology) {
		log.debug(String.format("writeSequence: %s, %s, %d, %s",seqUuid.toString(), name, length, topology.toString()));
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into sequences (uuid, name, length, topology) values(?,?,?,?)");
			ps.setString(1, seqUuid.toString());
			ps.setString(2, name);
			ps.setInt(3, length);
			ps.setString(4, topology.toString());
			ps.execute();
			ps.close();
			
			ps = conn.prepareStatement("insert into datasets_sequences values (?,?);");
			ps.setString(1, datasetUuid.toString());
			ps.setString(2, seqUuid.toString());
			ps.execute();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (ps != null) {
				try {
					ps.close();
				}
				catch (Exception e) {
					log.error("Exception while closing statement", e);
				}
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Write multiple sequences in one transaction. Useful for genomes consisting
	 * of large numbers of unassembled fragments (scaffolds, contigs, whatever...)
	 */
	public void writeSequences(UUID datasetUuid, List<Sequence> sequences) {
		Connection conn = null;
		PreparedStatement psInsertSeq = null;
		PreparedStatement psDatasetSeq = null;
		try {
			conn = DriverManager.getConnection(connectString);
			psInsertSeq = conn.prepareStatement("insert into sequences (uuid, name, length, topology) values(?,?,?,?)");
			psDatasetSeq = conn.prepareStatement("insert into datasets_sequences values (?,?);");

			beginTransaction(conn);
			for (Sequence seq : sequences) {
				String uuid = seq.getUuid().toString();
				psInsertSeq.setString(1, uuid);
				psInsertSeq.setString(2, seq.getSeqId());
				psInsertSeq.setInt(3, seq.getLength());
				psInsertSeq.setString(4, seq.getTopology().toString());
				psInsertSeq.execute();

				psDatasetSeq.setString(1, datasetUuid.toString());
				psDatasetSeq.setString(2, uuid);
				psDatasetSeq.execute();
			}
			commitTransaction(conn);
		}
		catch (SQLException e) {
			try {
				rollbackTransaction(conn);
			}
			catch (Exception re) {
				log.error("Error rolling back transaction in writeSequences:" + re);
			}
			throw new RuntimeException(e);
		}
		finally {
			if (psInsertSeq != null) {
				try {
					psInsertSeq.close();
				}
				catch (Exception e) {
					log.error("Exception while closing psInsertSeq statement", e);
				}
			}
			if (psDatasetSeq != null) {
				try {
					psDatasetSeq.close();
				}
				catch (Exception e) {
					log.error("Exception while closing psDatasetSeq statement", e);
				}
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private void createTables(Connection conn) {
		Statement s = null;
		try {
			// read sql from resources
			String sqlStatements = FileUtils.readIntoString("sqlite_create_tables.sql");

			// assume statements are separated by a semicolon and a newline
			// not really correct, but easy
			String[] sqls = sqlStatements.split(";\n");

			beginTransaction(conn);

			s = conn.createStatement();
			for (String sql: sqls) {
				log.debug(sql);
				if (sql.trim().length() > 0)
					s.addBatch(sql);
			}
			s.executeBatch();

			commitTransaction(conn);
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (SQLException e1) {
					log.warn("Error rolling back transaction", e1);
				}
			}
			throw new RuntimeException("Error creating tables", e);
		}
		finally {
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
		}
	}

	// dependencies: connectString
	
	// ---- coordinate map methods --------------------------------------------
	
	/**
	 * Find a matching map from names to coordinates based on the
	 * given array of names. The heuristic used is that if 90% of
	 * the first 100 names match, we found a match.
	 * @return a CoordinateMap or null if none found
	 */
	public CoordinateMap findCoordinateMap(String[] names) {
		// TODO find mappings in mapping table
		List<String> tables = findTracksOfType("gene");
		for (String table : tables) {
			// test if names match gene names in table
			if (checkIfNamesMatch(names, table)) {
				// construct a map from names to coordinates
				return loadCoordinateMap(table);
			}
		}
		// return null if no mapping found
		return null;
	}

	/**
	 * Find matching maps from names to coordinates based on the
	 * given array of names. The heuristic used is that if 90% of
	 * the first 100 names match, we found a match.
	 * @return an array of matching table names
	 */
	public List<CoordinateMapSelection> findCoordinateMaps(String[] names) throws Exception {
		List<String> tables = findTracksOfType("gene");
		tables.addAll(getCoordinateMapTables());
		List<CoordinateMapSelection> maps = new ArrayList<CoordinateMapSelection>();
		for (String table : tables) {
			maps.add(getCoordinateMap(names, table));
		}
		Collections.sort(maps);
		return maps;
	}

	private List<String> findTracksOfType(String type) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select tracks.table_name from tracks where type=?");
			ps.setString(1, type);
			rs = ps.executeQuery();
			List<String> tables = new ArrayList<String>();
			while (rs.next()) {
				tables.add(rs.getString(1));
			}
			return tables;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing prepared statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	private boolean checkIfNamesMatch(String[] names, String table) {
		// return true if we hit greater than 90%
		return getMatchingPercentage(names, table) > 0.90;
	}

	private CoordinateMapSelection getCoordinateMap(String[] names, String table) {
		return new CoordinateMapSelection(table, getMatchingPercentage(names, table));
	}

	private double getMatchingPercentage(String[] names, String table) {
		int i;
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		
		if (names==null || names.length<1) return 0.0;

		try {
			// select count(*) from features_genes 
			// where name in ('VNG0001H', 'VNG0002G', 'VNG0003C', 'VNG1187G', 'VNG1173G');

			// construct query
			StringBuilder sql = new StringBuilder("select count(*) from " + table + " where name in (");
			sql.append("'").append(names[0]).append("'");
			for (i=1; i<100 && i<names.length; i++) {
				sql.append(",'").append(names[i]).append("'");
			}
			sql.append(");");
			log.debug(sql.toString());

			conn = DriverManager.getConnection(connectString);
			st = conn.createStatement();
			rs = st.executeQuery(sql.toString());
			if (rs.next()) {
				return (rs.getInt(1) / ((double)i));
			}
			return 0.0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (st != null)
					st.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * derive a coordinate map from a table
	 * @param table name of table containing coordinates and name
	 */
	public CoordinateMap loadCoordinateMap(String table) {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			st = conn.createStatement();
			rs = st.executeQuery(
					"select s.name as sequence_name, t.strand, t.start, t.end, t.name " +
					"from sequences as s join " + table + " as t on s.id = t.sequences_id " +
					"order by s.id, t.strand, t.start, t.end");
			Map<String, Coordinates> map = new HashMap<String, Coordinates>();
			while (rs.next()) {
				map.put(rs.getString(5), new Coordinates(
						rs.getString(1),
						Strand.fromString(rs.getString(2)),
						Math.min(rs.getInt(3), rs.getInt(4)),
						Math.max(rs.getInt(3), rs.getInt(4))));
			}
			// TODO use external implementation for large maps
			return new InMemoryCoordinateMap(map);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (st != null)
					st.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Implementation of CoordinateMap that stores mappings in memory. Probably
	 * not well suited for very large mappings.
	 */
	private class InMemoryCoordinateMap implements CoordinateMap {
		Map<String, Coordinates> map;
		boolean positional = false;

		InMemoryCoordinateMap(Map<String, Coordinates> map) {
			this.map = map;
		}

		public Coordinates getCoordinates(String name) {
			return map.get(name);
		}

		public boolean isPositional() {
			return positional;
		}
	}

	/**
	 * A CoordinateMap that looks each entry up in the DB as needed. Possibly
	 * better for large mappings.
	 */
	@SuppressWarnings("unused")
	private class SqlCoordinateMap implements CoordinateMap {
		String connectString;
		String table;
		public SqlCoordinateMap(String connectString, String table) {
			this.connectString = connectString;
			this.table = table;
		}
		public Coordinates getCoordinates(String name) {
			Connection conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				conn = DriverManager.getConnection(connectString);
				ps = conn.prepareStatement(
						"select s.name as sequence_name, t.strand, t.start, t.end " +
						"from sequences as s join " + table + " as t on s.id = t.sequences_id " +
						"where t.name = ?");
				ps.setString(1, name);
				rs = ps.executeQuery();
				if (rs.next()) {
					return new Coordinates(
							rs.getString(1),
							Strand.fromString(rs.getString(2)),
							Math.min(rs.getInt(3), rs.getInt(4)),
							Math.max(rs.getInt(3), rs.getInt(4)));
				}
				return null;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
			finally {
				try {
					if (rs != null)
						rs.close();
				}
				catch (Exception e1) {
					log.warn("Error closing result set", e1);
				}
				try {
					if (ps != null)
						ps.close();
				}
				catch (Exception e1) {
					log.warn("Error closing statement", e1);
				}
				try {
					if (conn != null)
						conn.close();
				}
				catch (Exception e1) {
					log.warn("Error closing connection", e1);
				}
			}
		}
		public boolean isPositional() {
			return false;
		}
	}

	public void createCoordinateMapping(UUID datasetUuid, String name, Iterable<NamedFeature> mappings) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);

			beginTransaction(conn);

			String validTableName = name.toLowerCase().replaceAll("[^a-z0-9]", "_");
			String table = uniquifyTableName(conn, "map_" + validTableName);
			createCoordinateMappingTable(conn, table);
			writeMapping(conn, mappings, new HeuristicSequenceMapper(loadSequenceMap(conn, datasetUuid)), table);
			
			commitTransaction(conn);
		}
		catch (Exception e) {
			log.error(e);
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public void createCoordinateMappingTable(Connection conn, String table) {
		Statement s = null;

		try {
			s = conn.createStatement();
			s.execute(
					"create table if not exists " + table +
					"(name text primary key not null," +
					"sequences_id integer not null," +
					"strand text not null," +
					"start integer not null," +
					"end integer not null);");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
		}
	}

	private List<String> getCoordinateMapTables() throws SQLException {
		Connection conn = null;
		ResultSet rs = null;
		List<String> tables = new ArrayList<String>();

		try {
			conn = DriverManager.getConnection(connectString);
			DatabaseMetaData dbmd = conn.getMetaData();

			rs = dbmd.getTables(null, null, "map_%", null);
			while (rs.next()) {
				tables.add(rs.getString(3).toLowerCase());
			}
			return tables;
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e) {
				log.warn("Error closing result set", e);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * For names generated from user input, we need to ensure the uniqueness of
	 * the name. Here, we do that by looking for similarly named tables and appending
	 * a number to the proposed name, if necessary.
	 */
	private String uniquifyTableName(Connection conn, String table) throws SQLException {
		ResultSet rs = null;
		String tableRoot = table;

		// strip _\d+ suffix off of table name
		Matcher m = namePattern.matcher(table);
		if (m.matches()) {
			tableRoot = m.group(1);
		}

		try {
			DatabaseMetaData dbmd = conn.getMetaData();

			rs = dbmd.getTables(null, null, tableRoot + "%", null);
			int suffix = 0;
			boolean dup = false;
			while (rs.next()) {
				String existingTable = rs.getString(3).toLowerCase();
				if (table.equals(existingTable))
					dup = true;
				m = namePattern.matcher(existingTable);
				if (m.matches() && m.group(2) != null) {
					suffix = Math.max(suffix, Integer.parseInt(m.group(2)));
				}
			}
			rs.close();
			if (dup)
				return tableRoot + "_" + String.valueOf(suffix+1);
			return table;
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e) {
				log.warn("Error closing result set", e);
			}
		}
	}
	
	private Map<String, Integer> loadSequenceMap(Connection conn, UUID datasetUuid) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("select s.id, s.name " +
					"from sequences as s join datasets_sequences as d " +
					"on s.uuid = d.sequences_uuid " +
					"where d.datasets_uuid=?");
			ps.setString(1, datasetUuid.toString());
			rs = ps.executeQuery();
			Map<String, Integer> seqs = new HashMap<String, Integer>();
			while (rs.next()) {
				seqs.put(rs.getString(2), rs.getInt(1));
			}
			return seqs;
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e) {
				log.warn("Error closing result set", e);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement", e);
			}
		}
	}

	public void writeMapping(Connection conn, Iterable<NamedFeature> mappings, SequenceMapper<Integer> sequenceMapper, String table) throws SQLException {
		PreparedStatement ps = null;
		try {
			/*
 				(name text not null,
 				 sequences_id integer not null,
				 strand text not null,
				 start integer not null,
				 end integer not null)
			 */
			ps = conn.prepareStatement("insert into " + table + " values (?,?,?,?,?);");
			for (NamedFeature mapping : mappings) {
				int sequencesId = sequenceMapper.map(mapping.getSeqId());
				ps.setString(1, mapping.getName());
				ps.setInt(2, sequencesId);
				ps.setString(3, mapping.getStrand().toAbbreviatedString());
				ps.setInt(4, mapping.getStart());
				ps.setInt(5, mapping.getEnd());
				ps.executeUpdate();
			}
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement", e);
			}
		}
	}

	public void deleteCoordinateMap(String table) {
		Connection conn = null;
		Statement s = null;

		try {
			conn = DriverManager.getConnection(connectString);
			s = conn.createStatement();
			s.execute("drop table if exists " + table + ";");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * find collections of bookmarks for the given dataset
	 */
	public List<String> getBookmarkCollectionNames(UUID datasetUuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<String> names = new ArrayList<String>();

		try {
			conn = DriverManager.getConnection(connectString);

			// check if there is a bookmarks table
			DatabaseMetaData dbmd = conn.getMetaData();
			rs = dbmd.getTables(null, null, "bookmarks", null);
			if (!rs.next()) return names;
			rs.close();

			ps = conn.prepareStatement("select distinct(collectionName) from bookmarks order by collectionName;");
			rs = ps.executeQuery();
			while (rs.next()) {
				names.add(rs.getString(1));
			}
			return names;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public BookmarkDataSource loadBookmarks(String name) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Bookmark> bookmarks = new ArrayList<Bookmark>();

		try {
			conn = DriverManager.getConnection(connectString);
			
			ps = conn.prepareStatement("select s.name, b.strand, b.start, b.end, " +
					"b.name, b.annotation from bookmarks b join sequences s " +
					"on b.sequences_id=s.id where collectionName = ? " +
					"order by b.sequences_id, strand, start, end;");
			ps.setString(1, name);
			rs = ps.executeQuery();
			while (rs.next()) {
				bookmarks.add(new Bookmark(
						rs.getString(1),
						Strand.fromString(rs.getString(2)),
						rs.getInt(3),
						rs.getInt(4),
						rs.getString(5),
						rs.getString(6)));
			}
			return new ListBookmarkDataSource(name, bookmarks);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	/**
	 * Counts bookmarks with given name, mostly just to see if any exist
	 */
	public int countBookmarks(String collectionName, UUID datasetUuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = DriverManager.getConnection(connectString);
			
			if (tableExists(conn, "bookmarks")) {
				ps = conn.prepareStatement("select count(*) from bookmarks where collectionName = ?;");
				ps.setString(1, collectionName);
				rs = ps.executeQuery();
	
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
			return 0;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}

	public void writeBookmarks(BookmarkDataSource bookmarks, UUID datasetUuid) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		log.info("storing bookmarks " + bookmarks.getName() + " in dataset " + datasetUuid);

		try {
			conn = DriverManager.getConnection(connectString);

			Map<String, Integer> sequenceMap = loadSequenceMap(conn, datasetUuid);

			ensureBookmarksExists(conn);

			// overwrite existing bookmarks
			ps = conn.prepareStatement("delete from bookmarks where collectionName=?;");
			ps.setString(1, bookmarks.getName());
			ps.executeUpdate();
			ps.close();

			ps = conn.prepareStatement("insert into " +
					"bookmarks(collectionName, sequences_id, strand, start, end, name, sequence, annotation) " +
					"values(?, ?, ?, ?, ?, ?, ?, ?);");

			for (Bookmark bookmark : bookmarks) {
				ps.setString(1, bookmarks.getName());
				ps.setInt(2, sequenceMap.get(bookmark.getSeqId()));
				ps.setString(3, bookmark.getStrand().toAbbreviatedString());
				ps.setInt(4, bookmark.getStart());
				ps.setInt(5, bookmark.getEnd());
				ps.setString(6, bookmark.getLabel());
				ps.setString(7, bookmark.getSequence());
				ps.setString(8, bookmark.getAnnotation());
				ps.executeUpdate();
			}
			bookmarks.setDirty(false);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e1) {
				log.warn("Error closing result set", e1);
			}
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


	public void deleteBookmarks(String collectionName, UUID datasetUuid) {
		Connection conn = null;
		PreparedStatement ps = null;

		log.info("storing bookmarks " + collectionName + " in dataset " + datasetUuid);

		try {
			conn = DriverManager.getConnection(connectString);

			// no bookmarks, guess we're all done
			if (!tableExists(conn, "bookmarks")) {
				return;
			}

			ps = conn.prepareStatement("delete from bookmarks where collectionName = ?;");
			ps.setString(1, collectionName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


	public void ensureBookmarksExists(Connection conn) {
		Statement statement = null;

		log.info("ensuring that bookmarks table exists");

		try {

			statement = conn.createStatement();
			
//			statement.execute("create table if not exists datasets_bookmarks (" +
//							  "datasets_uuid text not null," +
//							  "bookmarks_collection_name text not null," + 
//							  "bookmarks_collection_id integer not null);");

			statement.execute("create table if not exists bookmarks (" +
							  "id integer primary key autoincrement," +
							  "collectionName text not null," +
							  "sequences_id integer not null," +
							  "strand text not null," +
							  "start integer not null," +
							  "end integer not null," +
							  "name text not null," +
							  "sequence text," + // for the new feature sequence/base
							  "annotation text);");
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e1) {
				log.warn("Error closing statement", e1);
			}
		}
	}


	public boolean tableExists(Connection conn, String tableName) {
		ResultSet rs = null;

		try {
			rs = conn.getMetaData().getTables(null, null, tableName, null);
			return rs.next();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
		}
	}

	
}
