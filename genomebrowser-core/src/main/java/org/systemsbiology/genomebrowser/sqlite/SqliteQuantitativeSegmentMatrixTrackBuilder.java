package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.FeatureSource;
import org.systemsbiology.genomebrowser.model.FeatureProcessor;
import org.systemsbiology.genomebrowser.model.FeatureFields;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.util.Iteratable;

/**
 *  Implementation of TrackBuilder for building quantitative segment matrix tracks
 */
public class SqliteQuantitativeSegmentMatrixTrackBuilder extends SqliteTrackBuilder {
	private static final Logger log = Logger.getLogger(SqliteQuantitativeSegmentMatrixTrackBuilder.class);
	int numColumns;

	/**
	 * set the number of value columns in the matrix
	 */
	public void setNumColumns(int numColumns) {
		this.numColumns = numColumns;
	}

	public void addFeatures(Iteratable<Feature> features) {
		if (numColumns < 1) {
			throw new RuntimeException("numColumns must be set prior to adding features to a matrix track.");
		}
		int n = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			beginTransaction(conn);

			// build an insert statement with slots for sequence, strand, start, end, and numColumns values
			StringBuilder sql = new StringBuilder("insert into " + source + " values (?,?,?,?");
			for (int i=0; i<numColumns; i++) {
				sql.append(",?");
			}
			sql.append(");");

			ps = conn.prepareStatement(sql.toString());

			for (Feature f : features) {
				Feature.Matrix fm = (Feature.Matrix)f;
				ps.setString(1, fm.getSeqId());
				ps.setString(2, fm.getStrand().toAbbreviatedString());
				ps.setInt(3, fm.getStart());
				ps.setInt(4, fm.getEnd());
				int i=5;
				double[] values = fm.getValues();
				if (values.length != numColumns) {
					throw new RuntimeException("mismatched number of value columns in matrix track");
				}
				for (double value : values) {
					ps.setDouble(i++, value);
				}
				ps.executeUpdate();
				n++;
				if (n%100==0)
					progress.fireIncrementProgressEvent(100);
			}
			commitTransaction(conn);
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

	public void addFeatures(FeatureSource featureSource) {
		if (numColumns < 1) {
			throw new RuntimeException("numColumns must be set prior to adding features to a matrix track.");
		}
		Connection conn = null;
		FeatureProcessor fp = null;
		try {
			conn = DriverManager.getConnection(connectString);
			fp = new QuantitativeSegmentMatrixFeatureProcessor(conn, numColumns);
			featureSource.processFeatures(fp);
			log.info("imported " + fp.getCount() + " feature" + (fp.getCount()==1 ? "" : "s") + " into temp table.");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			fp.cleanup();
			try {
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}


	public void processFeatures() {
		convertStrandNames();
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			// TODO should be in a transaction?

			// set number of value columns
			setNumColumns(attributes.getInt("matrix.num.cols", 0));
			if (numColumns < 1) {
				throw new RuntimeException("numColumns must be set prior to adding features to a matrix track.");
			}

			// create a unique table name
			table = uniquifyTableName(conn, toFeaturesTableName(name));

			// create and fill features table
			createQuantitativeSegmentMatrixFeaturesTable(conn, table);
			copyQuantitativeSegmentFeaturesIntoFeaturesTable(conn, table, datasetUuid);
			
			// add metadata
			uuid = createNewTrackEntry(conn, name, type, table);
			assignTracksToDataset(conn, datasetUuid, uuid);
			
			// process and add attributes
			applyOverlay(attributes, datasetUuid);
			ds.writeAttributes(conn, uuid, attributes);
		}
		catch (Exception e) {
			throw new RuntimeException("Error processing quantitative segment features", e);
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

	public Track<Feature> getFinishedTrack() {
		return ds.loadTrack(uuid);
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


	public void createQuantitativeSegmentMatrixFeaturesTable(Connection conn, String table) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			StringBuilder sql = new StringBuilder("create table ");
			sql.append(table).append(" (");
			sql.append("sequences_id integer NOT NULL,");
			sql.append("strand text NOT NULL,");
			sql.append("start integer NOT NULL,");
			sql.append("end integer NOT NULL");
			for (int i=0; i<numColumns; i++) {
				sql.append(", value").append(i).append(" numeric");
			}
			sql.append(");");
	
			statement.execute(sql.toString());
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

	public void copyQuantitativeSegmentFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid) throws SQLException {
		Statement statement = null;

		try {
			statement = conn.createStatement();
			Set<String> unknownSequences = checkSequenceNames(conn, source);
			if (unknownSequences.size() > 0)
				throw new RuntimeException("Unknown sequences: " + unknownSequences);

			StringBuilder valueColumns = new StringBuilder();
			for (int i=0; i<numColumns; i++) {
				valueColumns.append(", t.value").append(i);
			}
			
			statement.execute("insert into " + table + " " +
					"select s.id, t.strand, t.start, t.end" +
					valueColumns.toString() + " " +
					"from " + source + " as t " +
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
}
