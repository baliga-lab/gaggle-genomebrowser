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
 * Implementation of TrackBuilder for building quantitative positional tracks
 */
public class SqliteQuantitativePositionalTrackBuilder extends SqliteTrackBuilder {
	private static final Logger log = Logger.getLogger(SqliteQuantitativePositionalTrackBuilder.class);

	public void addFeatures(Iteratable<Feature> features) {
		int n = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			beginTransaction(conn);
			// sequences_name, strand, position, value
			ps = conn.prepareStatement("insert into " + source + " values (?,?,?,?);");

			for (Feature f : features) {
				Feature.Quantitative fq = (Feature.Quantitative)f;
				ps.setString(1, fq.getSeqId());
				ps.setString(2, fq.getStrand().toAbbreviatedString());
				ps.setInt(3, fq.getCentralPosition());
				ps.setDouble(4, fq.getValue());
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
		Connection conn = null;
		FeatureProcessor fp = null;
		try {
			conn = DriverManager.getConnection(connectString);
			fp = new QPFeatureProcessor(conn);
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

			// create a unique table name
			table = uniquifyTableName(conn, toFeaturesTableName(name));

			// create and fill features table
			createFeaturesTable(conn, table);
			copyFeaturesIntoFeaturesTable(conn, table, datasetUuid);
			
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




	class QPFeatureProcessor implements FeatureProcessor {
		PreparedStatement ps;
		Connection conn;
		int count;

		public QPFeatureProcessor(Connection conn) throws SQLException {
			this.conn = conn;

			// fields: sequences_name, strand, start, end, value
			// we'll have to resolve sequence ids later
			ps = conn.prepareStatement("insert into " + source + " values (?,?,?,?);");
		}

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
				log.warn("Error closing statement in quantitative positional feature processor.cleanup(...)", e);
			}
		}
	}

	public void createFeaturesTable(Connection conn, String table) throws SQLException {
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

	public void copyFeaturesIntoFeaturesTable(Connection conn, String table, UUID datasetUuid) throws SQLException {
		Statement statement = null;
		
		try {
			statement = conn.createStatement();
			Set<String> unknownSequences = checkSequenceNames(conn, source);
			if (unknownSequences.size() > 0)
				throw new RuntimeException("Unknown sequences: " + unknownSequences);
			statement.execute("insert into " + table + " " +
					"select s.id, t.strand, t.position, t.value " +
					"from " + source + " as t " +
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
}
