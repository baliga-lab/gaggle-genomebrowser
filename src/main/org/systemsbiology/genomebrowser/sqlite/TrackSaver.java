package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.GeneFeature;
import org.systemsbiology.genomebrowser.model.Track;


// TODO TrackSaver is broken -- is there any need for this?
// It's used by TrackVisualPropertiesEditor.

/**
 * Persists tracks of several types to a SQLite database.
 * @author cbare
 */
public class TrackSaver extends SqliteDb {
	private static final Logger log = Logger.getLogger(TrackSaver.class);
	private Connection conn;
	private Statement stmnt;
	private PreparedStatement ps;
	private int count;
	private String connectString;


	public TrackSaver(String connectString) {
		this.connectString = connectString;
		SqliteDataSource.loadSqliteDriver();
	}

	public void setFilename(String filename) {
		this.connectString = SqliteDataSource.getConnectStringForFile(filename);
	}

	private void executeBatch() throws SQLException {
		conn.setAutoCommit(false);
		ps.executeBatch();
		conn.commit();
		conn.setAutoCommit(true);
		count = 0;
	}

	/*
	 * 	tracks_id
	 * 	sequences_id
	 * 	strand
	 * 	start
	 * 	end
	 * 	value
	 * 	name
	 * 	common_name
	 * 	annotation
	 * 
	 */
	
	/*
		CREATE TABLE features_transcript_signal (
			sequences_id integer NOT NULL,
			strand text NOT NULL,
			start integer NOT NULL,
			end integer NOT NULL,
			value numeric);
	 */

	public void saveQuantitativeTrackToTable(Track.Quantitative<? extends Feature.Quantitative> track, String table) {
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into features values (?, ?, ?, ?, ?, ?, null, null, null);");

			for (Feature.Quantitative feature : track.features()) {
				ps.setString(1, track.getUuid().toString());
				ps.setString(2, feature.getSeqId());
				ps.setString(3, feature.getStrand().toAbbreviatedString());
				ps.setInt(4, feature.getStart());
				ps.setInt(5, feature.getEnd());
				ps.setDouble(6, feature.getValue());
				ps.addBatch();
				count++;
				if (count >= 100) {
					executeBatch();
				}
			}
			if (count > 0) {
				executeBatch();
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
				if (stmnt != null)
					stmnt.close();
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

	
	/*
		CREATE TABLE features_peaks_chip_chip_tfbd_nimb (
			sequences_id integer NOT NULL,
			strand text NOT NULL,
			position integer NOT NULL,
			value numeric);
	 */
	
	public void savePositionalTrackToTable(Track.Quantitative<Feature.Quantitative> track, String table) {
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into features values (?, ?, ?, ?, null, ?, null, null, null);");

			for (Feature.Quantitative feature : track.features()) {
				ps.setString(1, track.getUuid().toString());
				ps.setString(2, feature.getSeqId());
				ps.setString(3, feature.getStrand().toAbbreviatedString());
				ps.setInt(4, feature.getCentralPosition());
				ps.setDouble(5, feature.getValue());
				ps.addBatch();
				count++;
				if (count >= 100) {
					executeBatch();
				}
			}
			if (count > 0) {
				executeBatch();
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
				if (stmnt != null)
					stmnt.close();
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


	/*
		CREATE TABLE features_genes (
			sequences_id integer NOT NULL,
			strand text NOT NULL,
			start integer NOT NULL,
			end integer NOT NULL,
			name text,
			common_name text,
			gene_type text);
	 */
	
	public void saveGeneTrackToTable(Track.Gene<GeneFeature> track, String table) {
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into features values (?, ?, ?, ?, ?, null, ?, ?, null);");

			for (GeneFeature feature : track.features()) {
				ps.setString(1, track.getUuid().toString());
				ps.setString(2, feature.getSeqId());
				ps.setString(3, feature.getStrand().toAbbreviatedString());
				ps.setInt(4, feature.getStart());
				ps.setInt(5, feature.getEnd());
				ps.setString(6, feature.getName());
				ps.setString(7, feature.getCommonName());
				ps.addBatch();
				count++;
				if (count >= 100) {
					executeBatch();
				}
			}
			if (count > 0) {
				executeBatch();
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
				if (stmnt != null)
					stmnt.close();
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
	 * Save the track name and attributes back to the database.
	 */
	public void updateTrack(Track<? extends Feature> track) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = DriverManager.getConnection(connectString);
			beginTransaction(conn);

			// update track name
			ps = conn.prepareStatement("update tracks set name=? where uuid=?");
			ps.setString(1, track.getName());
			ps.setString(2, track.getUuid().toString());
			ps.execute();
			ps.close();

			// remove existing attributes
			ps = conn.prepareStatement("delete from attributes where uuid=?");
			ps.setString(1, track.getUuid().toString());
			ps.execute();
			ps.close();

			ps = conn.prepareStatement("insert into attributes values(?,?,?)");

			for (String key: track.getAttributes().keySet()) {
				ps.setString(1, track.getUuid().toString());
				ps.setString(2, key);
				ps.setObject(3, track.getAttributes().get(key));
				ps.execute();
			}
			
			commitTransaction(conn);
		}
		catch (Exception e) {
			if (conn != null) {
				try {
					rollbackTransaction(conn);
				}
				catch (Exception e1) {
					log.error(e1);
				}
			}
			log.warn(e);
			throw new RuntimeException("Error writing track attributes to Sqlite DB", e);
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
			if (conn != null) {
				try {
					conn.close();
				}
				catch (SQLException e) {
					log.error("Error closing connection", e);
				}
			}
		}
	}
}
