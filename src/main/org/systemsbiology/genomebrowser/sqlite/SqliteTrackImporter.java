package org.systemsbiology.genomebrowser.sqlite;

import java.awt.Color;
import java.io.File;
import java.sql.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.genomebrowser.app.ProgressListenerWrapper;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.TrackStub;
import org.systemsbiology.genomebrowser.ui.importtrackwizard.TrackImporter;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.StringUtils;


/**
 * Imports tracks to a dataset stored in a sqlite database
 * @author cbare
 */
public class SqliteTrackImporter extends SqliteDb implements TrackImporter {
	private static final Logger log = Logger.getLogger(SqliteTrackImporter.class);
	private static final Pattern namePattern = Pattern.compile("(.*?)(?:_(\\d+))?");
	private String connectString;
	private ProgressListenerSupport progressListeners = new ProgressListenerSupport();
	private SqliteDataSource dataSource;


	public SqliteTrackImporter(File file) {
		this(getConnectStringForFile(file.getAbsolutePath()));
	}

	public SqliteTrackImporter(String connectString) {
		this.connectString = connectString;
		SqliteDataSource.loadSqliteDriver();
		dataSource = new SqliteDataSource(connectString);
		// spit out progress messages to the log
		progressListeners.addProgressListener(new ProgressListener() {
			public void done() {
				log.info("SqliteTrackImporter done");
			}
			public void setMessage(String message) {
				log.info(message);
			}
			public void incrementProgress(int amount) {}
			public void setExpectedProgress(int expected) {}
			public void setProgress(int progress) {}
		});
	}

	public static String getConnectStringForFile(String filename) {
		if (StringUtils.isNullOrEmpty(filename))
			throw new RuntimeException("Can't pass an empty filename to getConnectStringForFile(filename).");
		return "jdbc:sqlite:" + filename;
	}



	// The general procedure (steps) for importing track data is:
	// 1. create temp table
	// 2. read features from source into temp table
	// 3. resolve sequence names to entries in the sequences table??
	// 4. select features into track table ordering by sequence, strand, start, end
	// 5. create entry in tracks table

	/**
	 * Accepts a name, dataset uuid, and a source of features and creates a
	 * quantitative segment track. The features do not need to be in any particular
	 * order as they will be loaded into a temp table and sorted on their way
	 * into the database.
	 * @param name the name of the new track
	 * @param datasetUuid uuid of dataset to which this track will belong
	 * @param featureSource source of features
	 * @return the UUID of the new track
	 */
	public UUID importQuantitativeSegmentTrack(String name, UUID datasetUuid, FeatureSource featureSource) {
		progressListeners.fireSetExpectedProgressEvent(100);
		progressListeners.fireMessageEvent("importing track: " + name + " to db " + connectString);

		log.info("importing track: " + name + " to db " + connectString);

		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			beginTransaction(conn);

			//name = uniquifyTrackName(conn, name);
			String table = uniquifyTableName(conn, toFeaturesTableName(name));
			progressListeners.fireProgressEvent(5);

			progressListeners.fireMessageEvent("creating temp table");
			createQuantitativeSegmentTempTable(conn);
			progressListeners.fireProgressEvent(10);

			// set up wrapper progress listener for scale and forward progress events
			ProgressListenerWrapper plw = new ProgressListenerWrapper(progressListeners);
			plw.scaleProgressToFit(10, 70);
			featureSource.addProgressListener(plw);

			progressListeners.fireMessageEvent("reading features");
			importQuantitativeSegmentFeatures(conn, featureSource);
			progressListeners.fireProgressEvent(70);

			progressListeners.fireMessageEvent("creating table " + table);
			createQuantitativeSegmentFeaturesTable(conn, table);
			progressListeners.fireProgressEvent(75);

			progressListeners.fireMessageEvent("copying features into " + table);
			copyQuantitativeSegmentFeaturesIntoFeaturesTable(conn, table, datasetUuid);
			progressListeners.fireProgressEvent(90);

			progressListeners.fireMessageEvent("creating track record and linking to dataset");
			UUID trackUuid = createNewTrackEntry(conn, name, "quantitative.segment", table);
			assignTracksToDataset(conn, datasetUuid, trackUuid);
			progressListeners.fireProgressEvent(100);
			
			commitTransaction(conn);

			int count = getFeatureCount(trackUuid);
			String msg = String.format("finished importing track: %s (%,d features)", name, count);
			progressListeners.fireMessageEvent(msg);
			log.info(msg);

			return trackUuid;
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (SQLException e1) {
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

	public void createQuantitativeSegmentTempTable(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create temp table temp (" +
					"sequences_name text NOT NULL," +
					"strand text NOT NULL," +
					"start integer NOT NULL," +
					"end integer NOT NULL," +
					"value numeric);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativeSegmentTempTable(...)", e);
			}
		}
	}

	public void createQuantitativeSegmentFeaturesTable(Connection conn, String table) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create table " + table + " (" +
					"sequences_id integer NOT NULL," +
					"strand text NOT NULL," +
					"start integer NOT NULL," +
					"end integer NOT NULL," +
					"value numeric);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativeSegmentFeaturesTable(...)", e);
			}
		}
	}

	/**
	 * read quantitative segment features into the temp table associated with the given connection.
	 * Quantitative segment features have a start, end, and a floating point value.
	 * 
	 * NOTE: the down-side of this approach is that when we're done, we need a way to tell the
	 * featureSource to do any necessary cleanup (closing files, or whatever).
	 */
	public void importQuantitativeSegmentFeatures(Connection conn, Iterable<Feature.Quantitative> featureSource) {
		PreparedStatement ps = null;

		try {
			// fields: sequences_name, strand, start, end, value
			// we'll have to resolve sequences later
			ps = conn.prepareStatement("insert into temp values (?,?,?,?,?);");

			for (Feature.Quantitative feature : featureSource) {
				ps.setString(1, feature.getSeqId());
				ps.setString(2, feature.getStrand().toAbbreviatedString());
				ps.setInt(3, feature.getStart());
				ps.setInt(4, feature.getEnd());
				ps.setDouble(5, feature.getValue());
				ps.executeUpdate();
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing prepared statement", e);
			}
		}
	}

	public void importQuantitativeSegmentFeatures(FeatureSource featureSource) throws Exception {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			importQuantitativeSegmentFeatures(conn, featureSource);
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

	public void importQuantitativeSegmentFeatures(Connection conn, FeatureSource featureSource) throws Exception {
		FeatureProcessor fp = new QuantitativeSegmentFeatureProcessor(conn);
		featureSource.processFeatures(fp);
		log.info("imported " + fp.getCount() + " feature" + (fp.getCount()==1 ? "" : "s") + " into temp table.");
		fp.cleanup();
	}

	class QuantitativeSegmentFeatureProcessor implements FeatureProcessor {
		PreparedStatement ps;
		Connection conn;
		int count;

		public QuantitativeSegmentFeatureProcessor(Connection conn) throws SQLException {
			this.conn = conn;

			// fields: sequences_name, strand, start, end, value
			// we'll have to resolve sequence ids later
			ps = conn.prepareStatement("insert into temp values (?,?,?,?,?);");
		}

		public void process(FeatureFields fields) throws SQLException {
			ps.setString(1, fields.getSequenceName());
			ps.setString(2, fields.getStrand());
			ps.setInt(3, fields.getStart());
			ps.setInt(4, fields.getEnd());
			ps.setDouble(5, fields.getValue());
			ps.executeUpdate();
			count++;
		}

		public int getCount() {
			return count;
		}

		public void cleanup() {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement in QuantitativeSegmentFeatureProcessor.cleanup(...)", e);
			}
		}
	}

	public void copyQuantitativeSegmentFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid) throws SQLException {
		Statement statement = null;
		
		// TODO should limit to sequences in the current dataset

		try {
			statement = conn.createStatement();
			Set<String> unknownSequences = checkSequenceNames(conn);
			if (unknownSequences.size() > 0)
				throw new RuntimeException("Unknown sequences: " + unknownSequences);
			statement.execute("insert into " + table + " " +
					"select s.id, t.strand, t.start, t.end, t.value " +
					"from temp as t " +
					"join sequences as s on s.name = t.sequences_name " +
					"where s.uuid in " +
					"  (select sequences_uuid " +
					"   from datasets_sequences " +
					"   where datasets_uuid='" + datasetUuid.toString() + "') " +
					"order by s.id, t.strand, t.start, t.end;");
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



	// ---- quantitative positional track functions ---------------------------

	public UUID importQuantitativePositionalTrack(String name, UUID datasetUuid, FeatureSource featureSource) {
		progressListeners.fireSetExpectedProgressEvent(100);
		progressListeners.fireMessageEvent("importing track: " + name + " to db " + connectString);

		log.info("importing track: " + name + " to db " + connectString);

		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			beginTransaction(conn);

			//name = uniquifyTrackName(conn, name);
			String table = uniquifyTableName(conn, toFeaturesTableName(name));
			progressListeners.fireProgressEvent(5);

			progressListeners.fireMessageEvent("creating temp table");
			createQuantitativePositionalTempTable(conn);
			progressListeners.fireProgressEvent(10);

			// set up wrapper progress listener for scale and forward progress events
			ProgressListenerWrapper plw = new ProgressListenerWrapper(progressListeners);
			plw.scaleProgressToFit(10, 70);
			featureSource.addProgressListener(plw);

			progressListeners.fireMessageEvent("reading features");
			importQuantitativePositionalFeatures(conn, featureSource);
			progressListeners.fireProgressEvent(70);

			progressListeners.fireMessageEvent("creating table " + table);
			createQuantitativePositionalFeaturesTable(conn, table);
			progressListeners.fireProgressEvent(75);

			progressListeners.fireMessageEvent("copying features into " + table);
			copyQuantitativePositionalFeaturesIntoFeaturesTable(conn, table, datasetUuid);
			progressListeners.fireProgressEvent(90);

			progressListeners.fireMessageEvent("creating track record and linking to dataset");
			UUID trackUuid = createNewTrackEntry(conn, name, "quantitative.positional", table);
			assignTracksToDataset(conn, datasetUuid, trackUuid);
			progressListeners.fireProgressEvent(100);
			
			commitTransaction(conn);

			int count = getFeatureCount(trackUuid);
			String msg = String.format("finished importing track: %s (%,d features)", name, count);
			progressListeners.fireMessageEvent(msg);
			log.info(msg);

			return trackUuid;
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (SQLException e1) {
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

	public void createQuantitativePositionalTempTable(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create temp table temp (" +
					"sequences_name text NOT NULL," +
					"strand text NOT NULL," +
					"position integer NOT NULL," +
					"value numeric);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativePositionalTempTable(...)", e);
			}
		}
	}

	public void createQuantitativePositionalFeaturesTable(Connection conn, String table) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create table " + table + " (" +
					"sequences_id integer NOT NULL," +
					"strand text NOT NULL," +
					"position integer NOT NULL," +
					"value numeric);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativePositionalFeaturesTable(...)", e);
			}
		}
	}

	public void importQuantitativePositionalFeatures(FeatureSource featureSource) throws Exception {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			importQuantitativePositionalFeatures(conn, featureSource);
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

	public void importQuantitativePositionalFeatures(Connection conn, FeatureSource featureSource) throws Exception {
		FeatureProcessor fp = new QuantitativePositionalFeatureProcessor(conn);
		featureSource.processFeatures(fp);
		log.info("imported " + fp.getCount() + " feature" + (fp.getCount()==1 ? "" : "s") + " into temp table.");
		fp.cleanup();
	}

	class QuantitativePositionalFeatureProcessor implements FeatureProcessor {
		PreparedStatement ps;
		Connection conn;
		int count;

		public QuantitativePositionalFeatureProcessor(Connection conn) throws SQLException {
			this.conn = conn;

			// fields: sequences_name, strand, position, value
			// we'll have to resolve sequence ids later
			ps = conn.prepareStatement("insert into temp values (?,?,?,?);");
		}

		// is averaging start and end the best thing to do here? Or should we
		// just use start as position?

		public void process(FeatureFields fields) throws SQLException {
			ps.setString(1, fields.getSequenceName());
			ps.setString(2, fields.getStrand());
			ps.setInt(3, fields.getPosition());
			ps.setDouble(4, fields.getValue());
			ps.executeUpdate();
			count++;
		}

		public int getCount() {
			return count;
		}

		public void cleanup() {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement in QuantitativePositionalFeatureProcessor.cleanup(...)", e);
			}
		}
	}

	public void copyQuantitativePositionalFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid) throws SQLException {
		Statement statement = null;

		// TODO should limit to sequences in the current dataset

		try {
			statement = conn.createStatement();
			Set<String> unknownSequences = checkSequenceNames(conn);
			if (unknownSequences.size() > 0)
				throw new RuntimeException("Unknown sequences: " + unknownSequences);
			statement.execute("insert into " + table + " " +
					"select s.id, t.strand, t.position, t.value " +
					"from temp as t " +
					"join sequences as s on s.name = t.sequences_name " +
					"where s.uuid in " +
					"  (select sequences_uuid " +
					"   from datasets_sequences " +
					"   where datasets_uuid='" + datasetUuid.toString() + "') " +
					"order by s.id, t.strand, t.position;");
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



	// ---- gene track functions ----------------------------------------------

	public UUID importGeneTrack(String name, UUID datasetUuid, FeatureSource featureSource) {
		progressListeners.fireSetExpectedProgressEvent(100);
		progressListeners.fireMessageEvent("importing track: " + name + " to db " + connectString);

		log.info("importing track: " + name + " to db " + connectString);

		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			beginTransaction(conn);

			//name = uniquifyTrackName(conn, name);
			String table = uniquifyTableName(conn, toFeaturesTableName(name));
			progressListeners.fireProgressEvent(5);

			progressListeners.fireMessageEvent("creating temp table");
			createGeneTempTable(conn);
			progressListeners.fireProgressEvent(10);

			// set up wrapper progress listener for scale and forward progress events
			ProgressListenerWrapper plw = new ProgressListenerWrapper(progressListeners);
			plw.scaleProgressToFit(10, 70);
			featureSource.addProgressListener(plw);

			progressListeners.fireMessageEvent("reading features");
			importGeneFeatures(conn, featureSource);
			progressListeners.fireProgressEvent(70);

			progressListeners.fireMessageEvent("creating table " + table);
			createGeneFeaturesTable(conn, table);
			progressListeners.fireProgressEvent(75);

			progressListeners.fireMessageEvent("copying features into " + table);
			copyGeneFeaturesIntoFeaturesTable(conn, table, datasetUuid);
			progressListeners.fireProgressEvent(90);

			progressListeners.fireMessageEvent("creating track record and linking to dataset");
			UUID trackUuid = createNewTrackEntry(conn, name, "gene", table);
			assignTracksToDataset(conn, datasetUuid, trackUuid);
			progressListeners.fireProgressEvent(100);
			
			commitTransaction(conn);

			int count = getFeatureCount(trackUuid);
			String msg = String.format("finished importing track: %s (%,d features)", name, count);
			progressListeners.fireMessageEvent(msg);
			log.info(msg);

			return trackUuid;
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (SQLException e1) {
					log.error(e1);
				}
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			else
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

	// TODO avoid NOT NULLs in temp table?
	public void createGeneTempTable(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create temp table temp (" +
					"sequences_name text NOT NULL, " +
					"strand text NOT NULL, " +
					"start integer NOT NULL, " +
					"end integer NOT NULL, " +
					"name text NOT NULL, " +
					"common_name text, " +
					"gene_type text);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativeSegmentTempTable(...)", e);
			}
		}
	}

	public void createGeneFeaturesTable(Connection conn, String table) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("create table " + table + " (" +
					"sequences_id integer NOT NULL, " +
					"strand text NOT NULL, " +
					"start integer NOT NULL, " +
					"end integer NOT NULL, " +
					"name text NOT NULL, " +
					"common_name text, " +
					"gene_type text NOT NULL);");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement in createQuantitativeSegmentTempTable(...)", e);
			}
		}
	}

	public void importGeneFeatures(Connection conn, FeatureSource featureSource) throws Exception {
		FeatureProcessor fp = new GeneFeatureProcessor(conn);
		featureSource.processFeatures(fp);
		log.info("imported " + fp.getCount() + " gene feature" + (fp.getCount()==1 ? "" : "s") + " into temp table.");
		fp.cleanup();
	}

	class GeneFeatureProcessor implements FeatureProcessor {
		PreparedStatement ps;
		Connection conn;
		int count;

		public GeneFeatureProcessor(Connection conn) throws SQLException {
			this.conn = conn;

			// fields: sequences_name, strand, start, end, name, common_name, gene_type
			// we'll have to resolve sequence ids later
			ps = conn.prepareStatement("insert into temp values (?,?,?,?,?,?,?);");
		}

		public void process(FeatureFields fields) throws SQLException {
			try {
				ps.setString(1, fields.getSequenceName());
				ps.setString(2, fields.getStrand());
				ps.setInt(3, fields.getStart());
				ps.setInt(4, fields.getEnd());
				ps.setString(5, fields.getName());
				ps.setString(6, fields.getCommonName());
				ps.setString(7, fields.getGeneType());
				ps.executeUpdate();
				count++;
			}
			catch (SQLException e) {
				log.warn("Error importing feature: " + fields);
				throw e;
			}
		}

		public int getCount() {
			return count;
		}

		public void cleanup() {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement in GeneFeatureProcessor.cleanup(...)", e);
			}
		}
	}

	public void copyGeneFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid) throws SQLException {
		Statement statement = null;

		// TODO should limit to sequences in the current dataset
		// TODO copy features slow for lots of sequences
		
		// For the sea urchin genome we have ~114 thousand unassembled fragments called
		// scaffolds. This causes the join below to seriously dog out.

		try {
			statement = conn.createStatement();
			Set<String> unknownSequences = checkSequenceNames(conn);
			if (unknownSequences.size() > 0) {
				throw new RuntimeException("Unknown sequences: " + unknownSequences);
			}
			statement.execute("insert into " + table + " " +
					"select s.id, t.strand, t.start, t.end, t.name, t.common_name, t.gene_type " +
					"from temp as t " +
					"join sequences as s on s.name = t.sequences_name " +
					"where s.uuid in " +
					"  (select sequences_uuid " +
					"   from datasets_sequences " +
					"   where datasets_uuid='" + datasetUuid.toString() + "') " +
					"order by s.id, t.strand, t.start, t.end;");
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



	// ---- quantitative segment matrix track functions -----------------------

	/**
	 * Import a track which has sequence/strand/start/end coordinates and has one or
	 * more numeric value columns. Examples could be a set of microarray experiments
	 * with the same probes, such as might be received in a gaggle matrix broadcast.
	 * 
	 * NOTE: the FeatureSource is expected to pass in MatrixFeatureFields, rather than
	 * plain FeatureFields objects. This is not enforced, so be careful.
	 * 
	 * @param name the name of the new track
	 * @param datasetUuid uuid of dataset to which this track will belong
	 * @param featureSource source of MatrixFeatureFields objects
	 * @return the UUID of the new track
	 */
	public UUID importQuantitativeSegmentMatrixTrack(String name, UUID datasetUuid, FeatureSource featureSource, int columns) {
		progressListeners.fireSetExpectedProgressEvent(100);
		progressListeners.fireMessageEvent("importing track: " + name + " to db " + connectString);

		log.info("importing track: " + name + " to db " + connectString);

		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			beginTransaction(conn);

			//name = uniquifyTrackName(conn, name);
			String table = uniquifyTableName(conn, toFeaturesTableName(name));
			progressListeners.fireProgressEvent(5);

			progressListeners.fireMessageEvent("creating temp table");
			createQuantitativeSegmentMatrixTempTable(conn, columns);
			progressListeners.fireProgressEvent(10);

			// set up wrapper progress listener for scale and forward progress events
			ProgressListenerWrapper plw = new ProgressListenerWrapper(progressListeners);
			plw.scaleProgressToFit(10, 70);
			featureSource.addProgressListener(plw);

			progressListeners.fireMessageEvent("reading features");
			importQuantitativeSegmentMatrixFeatures(conn, featureSource, columns);
			progressListeners.fireProgressEvent(70);

			progressListeners.fireMessageEvent("creating table " + table);
			createQuantitativeSegmentMatrixFeaturesTable(conn, table, columns);
			progressListeners.fireProgressEvent(75);

			progressListeners.fireMessageEvent("copying features into " + table);
			copyQuantitativeSegmentMatrixFeaturesIntoFeaturesTable(conn, table, datasetUuid, columns);
			progressListeners.fireProgressEvent(90);

			progressListeners.fireMessageEvent("creating track record and linking to dataset");
			UUID trackUuid = createNewTrackEntry(conn, name, "quantitative.segment.matrix", table);
			assignTracksToDataset(conn, datasetUuid, trackUuid);
			progressListeners.fireProgressEvent(100);

			commitTransaction(conn);

			int count = getFeatureCount(trackUuid);
			String msg = String.format("finished importing track: %s (%,d features)", name, count);
			progressListeners.fireMessageEvent(msg);
			log.info(msg);

			return trackUuid;
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (SQLException e1) {
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

	public void createQuantitativeSegmentMatrixTempTable(Connection conn, int columns) throws SQLException {
		Statement statement = null;
		try {
			StringBuffer buffer = new StringBuffer(
					"create temp table temp (" +
					"sequences_name text NOT NULL," +
					"strand text NOT NULL," +
					"start integer NOT NULL," +
					"end integer NOT NULL");
			for (int i=0; i<columns; i++) {
				buffer.append(", ").append("value").append(i).append(" numeric");
			}
			buffer.append(")");
			statement = conn.createStatement();
			statement.execute(buffer.toString());
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement", e);
			}
		}
	}

	public void importQuantitativeSegmentMatrixFeatures(Connection conn, FeatureSource featureSource, int columns) throws Exception {
		FeatureProcessor fp = new QuantitativeSegmentMatrixFeatureProcessor(conn, columns);
		featureSource.processFeatures(fp);
		log.info("imported " + fp.getCount() + " feature" + (fp.getCount()==1 ? "" : "s") + " into temp table.");
		// TODO cleanup in finally block
		fp.cleanup();
	}

	class QuantitativeSegmentMatrixFeatureProcessor implements FeatureProcessor {
		PreparedStatement ps;
		Connection conn;
		int count;

		public QuantitativeSegmentMatrixFeatureProcessor(Connection conn, int columns) throws SQLException {
			this.conn = conn;

			// fields: sequences_name, strand, start, end, value1, ... , value<n>
			// we'll have to resolve sequence ids later
			StringBuilder sb = new StringBuilder("insert into temp values (?,?,?,?");
			for (int i=0; i<columns; i++) {
				sb.append(",?");
			}
			sb.append(");");
			ps = conn.prepareStatement(sb.toString());
		}

		public void process(FeatureFields fields) throws SQLException {
			ps.setString(1, fields.getSequenceName());
			ps.setString(2, fields.getStrand());
			ps.setInt(3, fields.getStart());
			ps.setInt(4, fields.getEnd());

			if (fields instanceof MatrixFeatureFields) {
				double[] values = ((MatrixFeatureFields)fields).getValues();
				int i=5;
				for (double d : values) {
					ps.setDouble(i++, d);
				}
			}
			else {
				ps.setDouble(5, fields.getValue());
			}

			ps.executeUpdate();
			count++;
		}

		public int getCount() {
			return count;
		}

		public void cleanup() {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement", e);
			}
		}
	}

	public void createQuantitativeSegmentMatrixFeaturesTable(Connection conn, String table, int columns) throws SQLException {
		Statement statement = null;
		try {
			StringBuffer sb = new StringBuffer(
					"create table " + table + " (" +
					"sequences_id integer NOT NULL," +
					"strand text NOT NULL," +
					"start integer NOT NULL," +
					"end integer NOT NULL");
			for (int i=0; i<columns; i++) {
				sb.append(", ").append("value").append(i).append(" numeric");
			}
			sb.append(")");
			statement = conn.createStatement();
			statement.execute(sb.toString());
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement", e);
			}
		}
	}

	public void copyQuantitativeSegmentMatrixFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid, int columns) throws SQLException {
		Statement statement = null;

		// TODO should limit to sequences in the current dataset

		try {
			Set<String> unknownSequences = checkSequenceNames(conn);
			if (unknownSequences.size() > 0)
				throw new RuntimeException("Unknown sequences: " + unknownSequences);

			StringBuilder sb = new StringBuilder("insert into ");
			sb.append(table).append(" select s.id, t.strand, t.start, t.end");
			for (int i=0; i<columns; i++) {
				sb.append(", ").append("t.value").append(i);
			}
			sb.append(" from temp as t " +
					"join sequences as s on s.name = t.sequences_name " +
					"where s.uuid in " +
					"  (select sequences_uuid " +
					"   from datasets_sequences " +
					"   where datasets_uuid='" + datasetUuid.toString() + "') " +
					"order by s.id, t.strand, t.start, t.end;");

			statement = conn.createStatement();
			statement.execute(sb.toString());
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

	// ---- end quantitative segment matrix track functions -------------------



	public int getFeatureCount(UUID trackUuid) {
		TrackStub stub = dataSource.loadTrackStub(trackUuid);
		return dataSource.getFeatureCount(stub.tableName);
	}

	public boolean deleteTrack(UUID trackUuid) {
		return dataSource.deleteTrack(trackUuid);
	}

	public Track<Feature> loadTrack(UUID trackUuid) {
		return dataSource.loadTrack(trackUuid);
	}

	public TrackStub checkIfTrackExists(Connection conn, String name) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = conn.prepareStatement("select uuid, name, type, table_name from tracks where name=?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			if (rs.next()) {
				return new TrackStub(
						UUID.fromString(rs.getString(1)),
						rs.getString(2),
						rs.getString(3),
						rs.getString(4));
			}
			else
				return null;
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
				log.error("Exception while closing statement", e);
			}
		}
	}

	public String uniquifyTrackName(Connection conn, String trackName) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String root = trackName;

		// strip _\d+ suffix off of table name
		Matcher m = namePattern.matcher(trackName);
		if (m.matches()) {
			root = m.group(1);
		}

		try {
			ps = conn.prepareStatement("select uuid, name, type, table_name from tracks where name like ?");
			ps.setString(1, root + "%");
			rs = ps.executeQuery();

			// if we do find similarly named tables, ensure a unique name by
			// appending a numeric suffix, if necessary.
			int suffix = 0;
			boolean dup = false;
			while (rs.next()) {
				if (trackName.equals(rs.getString(2)))
					dup = true;
				m = namePattern.matcher(rs.getString(2));
				if (m.matches() && m.group(2) != null) {
					suffix = Math.max(suffix, Integer.parseInt(m.group(2)));
				}
			}
			if (dup)
				return root + "_" + String.valueOf(suffix+1);
			return trackName;
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
				log.error("Exception while closing statement", e);
			}
		}
	}

	/**
	 * Make sure the table name is unique. The table name is derived by pre-
	 * pending "features_" to the track name. This should never be a problem if
	 * people choose reasonable table names, but we can't count on that, can we?
	 * @param conn database connection
	 * @param table proposed table name
	 * @return the table name possibly with a suffix of the form '_123'
	 */
	public String uniquifyTableName(Connection conn, String table) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String tableRoot = table;

		// strip _\d+ suffix off of table name
		Matcher m = namePattern.matcher(table);
		if (m.matches()) {
			tableRoot = m.group(1);
		}

		try {
			ps = conn.prepareStatement("select uuid, name, type, table_name from tracks where table_name like ?");
			ps.setString(1, tableRoot + "%");
			rs = ps.executeQuery();

			// if we do find similarly named tables, ensure a unique name by
			// appending a numeric suffix, if necessary.
			int suffix = 0;
			boolean dup = false;
			while (rs.next()) {
				if (table.equals(rs.getString(4)))
					dup = true;
				m = namePattern.matcher(rs.getString(4));
				if (m.matches() && m.group(2) != null) {
					suffix = Math.max(suffix, Integer.parseInt(m.group(2)));
				}
			}
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
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e) {
				log.error("Exception while closing statement", e);
			}
		}
	}

	/**
	 * Check if the sequence names present in the temp table are also
	 * present in the sequences table.
	 * @param conn
	 * @return a Set of unrecognized sequence names
	 * @throws SQLException
	 */
	private Set<String> checkSequenceNames(Connection conn) throws SQLException {
		Set<String> sequences = new TreeSet<String>(); 
		Statement statement = null;
		ResultSet rs = null;
		try {
			statement = conn.createStatement();
			rs = statement.executeQuery(
					"select distinct t.sequences_name " +
					"from temp as t " +
					"where t.sequences_name not in " +
					"(select s.name from sequences as s);");
			while (rs.next()) {
				sequences.add(rs.getString(1));
			}
			return sequences;
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
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.warn("Error closing statement", e);
			}
		}
	}

	/**
	 * @return the UUID of the newly created track
	 */
	public UUID createNewTrackEntry(Connection conn, String name, String type, String table) {
		PreparedStatement ps = null;

		// create a new UUID for track
		UUID uuid = UUID.randomUUID();

		try {
			// CREATE TABLE tracks (
			//   uuid text primary key not null,
			//   name text not null,
			//   type text not null,
			//   table_name text not null,
			//   strand text);

			ps = conn.prepareStatement("insert into tracks values (?,?,?,?);");
			ps.setString(1, uuid.toString());
			ps.setString(2, name);
			ps.setString(3, type);
			ps.setString(4, table);

			ps.executeUpdate();
			
			return uuid;
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

	public void assignTracksToDataset(Connection conn, UUID datasetUuid, UUID... trackUuids) {
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement("insert into datasets_tracks values (?,?);");

			for (UUID trackUuid: trackUuids) {
				ps.setString(1, datasetUuid.toString());
				ps.setString(2, trackUuid.toString());
				ps.executeUpdate();
			}
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


	public void storeAttributes(UUID uuid, Attributes attributes) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into attributes values (?,?,?);");

			for (String key: attributes.keySet()) {
				ps.setString(1, uuid.toString());
				ps.setString(2, key);
				Object obj = attributes.get(key);
				// TODO factor out representing attributes in DB.
				// This logic determines how various types of attributes will
				// be stored in the DB. This should be abstracted because it
				// will be needed elsewhere.
				if (obj instanceof Integer)
					ps.setInt(3, attributes.getInt(key));
				else if (obj instanceof Number)
					ps.setDouble(3, attributes.getDouble(key));
				else if (obj instanceof Color)
					ps.setInt(3, attributes.getColor(key).getRGB());
				// for some buggy reason, boolean false values were getting stored in the DB as zero (0).
				// storing them as strings works.
//				else if (obj instanceof Boolean)
//					ps.setBoolean(3, attributes.getBoolean(key));
				else
					ps.setString(3, attributes.getString(key));
				ps.executeUpdate();
			}
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

//	private Map<String, Integer> createSequenceMap() {
//		Connection conn = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//
//		try {
//			conn = DriverManager.getConnection(connectString);
//
//			Map<String, Integer> map = new HashMap<String, Integer>();
//
//			ps = conn.prepareStatement("select id, name from sequences;");
//			rs = ps.executeQuery();
//			while (rs.next()) {
//				int id = rs.getInt(1);
//				String name = rs.getString(2);
//				map.put(name, id);
//			}
//
//			return map;
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
	 * Make a non-rigorous effort to ensure the given string is a valid sql table name
	 * by replacing any non-alphanumeric characters with an underscore and converting
	 * to lower case.
	 */
	String toFeaturesTableName(String s) {
		s = s.toLowerCase().replaceAll("[^a-z0-9]", "_");
		return "features_" + s;
	}

	String toValidColumnName(String s) {
		return s.toLowerCase().replaceAll("[^a-z0-9]", "_");
	}

	public void addProgressListener(ProgressListener listener) {
		progressListeners.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListeners.removeProgressListener(listener);
	}
}
