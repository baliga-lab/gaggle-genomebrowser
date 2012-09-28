package org.systemsbiology.genomebrowser.sqlite;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.genomebrowser.io.track.CoordinateMapper;
import org.systemsbiology.genomebrowser.io.track.SequenceMapper;
import org.systemsbiology.genomebrowser.io.track.TrackBuilder;
import org.systemsbiology.genomebrowser.model.Feature;
import org.systemsbiology.genomebrowser.model.Track;
import org.systemsbiology.genomebrowser.sqlite.SqliteDataSource.TrackStub;
import org.systemsbiology.util.Attributes;
import org.systemsbiology.util.Iteratable;



// could I change the strategy of joining the temp features to the sequences table
// and do that as a separate step?

// should implementations based on track type be factored out as strategy objects?


public abstract class SqliteTrackBuilder extends SqliteDb implements TrackBuilder {
	private static final Logger log = Logger.getLogger(SqliteTrackBuilder.class);
	protected ProgressListenerSupport progress = new ProgressListenerSupport();
	protected String name;
	protected String type;
	protected String source;
	protected String table;
	protected Attributes attributes = new Attributes();

	protected UUID uuid;
	protected UUID datasetUuid;

	protected String connectString;
	protected SqliteDataSource ds;


	// dependency
	public void setConnectString(String connectString) {
		this.connectString = connectString;
	}

	// dependency
	public void setDatasetUuid(UUID uuid) {
		this.datasetUuid = uuid;
	}

	// dependency
	public void setSqliteDataSource(SqliteDataSource ds) {
		this.ds = ds;
	}


	public void startNewTrack(String name, String type) {
		this.name = name;
		this.type = type;
		this.source = "temp";
		this.uuid = null;
		this.table = null;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void applyCoordinateMapper(CoordinateMapper mapper) {
		// TODO Auto-generated method stub
	}

	public void applySequenceMapper(SequenceMapper mapper) {
		log.info("applying sequence mapper");
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			Set<String> names = checkSequenceNames(conn, source);
			//log.info("updating sequence names: " + names);

			ps = conn.prepareStatement("update " + source + " set sequences_name=? where sequences_name=?;");

			beginTransaction(conn);
			for (String name : names) {
				String standardName = mapper.map(name);
				log.info(name + " -> " + standardName);
				ps.setString(1, standardName);
				ps.setString(2, name);
				ps.execute();
			}
			commitTransaction(conn);
		}
		catch (Exception e) {
			try {
				rollbackTransaction(conn);
			}
			catch (Exception e2) {
				log.error(e2);
			}
			log.warn(e);
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
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

	public void convertStrandNames() {
		log.info("converting strand names");
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("update " + source + " set strand=? where lower(strand)=? or lower(strand)=? or lower(strand)=?;");

			ps.setString(1, "+");
			ps.setString(2, "forward");
			ps.setString(3, "for");
			ps.setString(4, "f");
			ps.execute();

			ps.setString(1, "-");
			ps.setString(2, "reverse");
			ps.setString(3, "rev");
			ps.setString(4, "r");
			ps.execute();
		}
		catch (Exception e) {
			log.warn(e);
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

	public void setAttributes(Attributes attributes) {
		this.attributes.putAll(attributes);
	}

	public Track<Feature> getFinishedTrack() {
		return null;
	}

	/**
	 * If the given attributes have an overlay set, find a track with the
	 * same overlay and copy it's attributes top, height, and (if not already set) rangeMin and rangeMax.
	 * @param attributes to copy to
	 * @param datasetUuid where to look for tracks to overlay onto (get attributes from)
	 */
	public void applyOverlay(Attributes attributes, UUID datasetUuid) {
		String overlayId = attributes.getString("overlay");
		if (overlayId != null) {
			for (TrackStub trackStub: ds.loadTrackStubs(datasetUuid)) {
				Attributes otherAttr = ds.getAttributes(trackStub.uuid);
				if (overlayId.equals(otherAttr.getString("overlay"))) {
					attributes.put("top", otherAttr.getString("top"));
					attributes.put("height", otherAttr.getString("height"));
					if (attributes.get("rangeMin")==null && otherAttr.containsKey("rangeMin"))
						attributes.put("rangeMin", otherAttr.getDouble("rangeMin"));
					if (attributes.get("rangeMax")==null && otherAttr.containsKey("rangeMax"))
						attributes.put("rangeMax", otherAttr.getDouble("rangeMax"));
					break;
				}
			}
		}
	}

	public void cancel() {
		Connection conn = null;
		Statement s = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);

			beginTransaction(conn);

			s = conn.createStatement();
			s.executeUpdate("drop table if exists " + table + ";");
			s.close();

			s = conn.createStatement();
			s.executeUpdate("drop table if exists " + source + ";");
			s.close();

			// delete entry from datasets_tracks
			ps = conn.prepareStatement("delete from datasets_tracks where tracks_uuid=?");
			ps.setString(1, uuid.toString());
			ps.executeUpdate();

			// delete entries from block_index
			ps = conn.prepareStatement("delete from block_index where tracks_uuid=?");
			ps.setString(1, uuid.toString());
			ps.executeUpdate();

			// delete attributes
			ps = conn.prepareStatement("delete from attributes where uuid=?");
			ps.setString(1, uuid.toString());
			ps.executeUpdate();

			// delete entry from track table
			ps = conn.prepareStatement("delete from tracks where uuid=?");
			ps.setString(1, uuid.toString());
			
			commitTransaction(conn);
		}
		catch (SQLException e) {
			try {
				rollbackTransaction(conn);
			}
			catch (Exception e2) {
				log.error(e2);
			}
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


	public void addProgressListener(ProgressListener listener) {
		progress.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progress.removeProgressListener(listener);
	}


	

	// ---- Quantitative Positional ------------------------------------------------------

	public void addQuantitativePositionalFeatures(Iteratable<Feature.Quantitative> features) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			
			beginTransaction(conn);
			ps = conn.prepareStatement("insert into " + source + " values (?,?,?,?);");

			for (Feature.Quantitative f : features) {
				ps.setString(1, f.getSeqId());
				ps.setString(2, f.getStrand().toAbbreviatedString());
				ps.setInt(3, f.getCentralPosition());
				ps.setDouble(5, f.getValue());
				ps.executeUpdate();
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
				if (conn != null)
					conn.close();
			}
			catch (Exception e1) {
				log.warn("Error closing connection", e1);
			}
		}
	}
	
	public void processQuantitativePositionalFeatures() {
		
	}



	// -----------------------------------------------------------------------------------

	/**
	 * Make a non-rigorous effort to ensure the given string is a valid sql table name
	 * by replacing any non-alphanumeric characters with an underscore and converting
	 * to lower case.
	 */
	protected String toFeaturesTableName(String s) {
		s = s.toLowerCase().replaceAll("[^a-z0-9]", "_");
		return "features_" + s;
	}


	/**
	 * split an numeric suffix off of a table name. For example,
	 * "my_table_123" would split into "my_table" and "123".
	 */
	private static final Pattern namePattern = Pattern.compile("(.*?)(?:_(\\d+))?");


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

	protected void assignTracksToDataset(Connection conn, UUID datasetUuid, UUID... trackUuids) {
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

	/**
	 * Check if the sequence names present in the temp table are also
	 * present in the sequences table.
	 * @param conn
	 * @return a Set of unrecognized sequence names
	 * @throws SQLException
	 */
	protected Set<String> checkSequenceNames(Connection conn, String table) throws SQLException {
		Set<String> sequences = new TreeSet<String>(); 
		Statement statement = null;
		ResultSet rs = null;
		try {
			statement = conn.createStatement();
			rs = statement.executeQuery(
					"select distinct t.sequences_name " +
					"from " + table + " as t " +
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
}
