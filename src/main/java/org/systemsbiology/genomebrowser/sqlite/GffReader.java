package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.io.LineReader;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * Read a GFF file into a sqlite table.
 * 
 * The import happens in two stages. First, we read all features into a temp
 * table. Then, we sort the features by sequence, strand, start, and end into
 * a features table. We also create an entry in the tracks table.
 * 
 * Client needs to specify: Name of track, name of feature table, and type of
 * track. The type of the track is determined by whether its features are
 * described by segments (start, end) or just a position on the sequence and
 * whether the features have labels, values or both.
 * 
 * Track types currently supported by GffReader are:
 *   quantitative.segment
 *   //quantitative.positional
 * 
 * Another issue is that sequence names given in the imported file might not
 * match our sequence names. 
 * 
 * @author cbare
 */
public class GffReader {
	private static final Logger log = Logger.getLogger(GffReader.class);
	LineReader loader;
	String connectString;


	public GffReader(String connectString) {
		loader = new LineReader(new GffLineProcessor());
		setConnectString(connectString);
	}

	// dependency
	private void setConnectString(String connectString) {
		try {
			Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		this.connectString = connectString;
	}

	public void read(String filename) throws Exception {
		createTempTable();
		loader.loadData(filename);
	}

	public void read(Reader reader) throws Exception {
		createTempTable();
		loader.loadData(reader);
	}

	public void read(File file) throws Exception {
		createTempTable();
		loader.loadData(file);
	}

	void createTempTable() {
		Connection conn = null;
		Statement s = null;
		try {
			conn = DriverManager.getConnection(connectString);
			s = conn.createStatement();
			s.execute("drop table if exists temp;");
			s.execute("create table temp ("
					+ "sequence text not null, "
					+ "strand text not null, "
					+ "start integer not null, "
					+ "end integer not null, "
					+ "label text not null, "
					+ "score numeric)");
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


	void dropTempTable() {
		Connection conn = null;
		Statement s = null;
		try {
			conn = DriverManager.getConnection(connectString);
			s = conn.createStatement();
			s.execute("drop table if exists temp;");
			// sqlite specific command to clean up unused space and defragment tables.
			s.execute("vacuum;");
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

	void insertFeatureIntoTemp(String seq, Strand strand, int start, int end, String label, double value) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("insert into temp values(?,?,?,?,?,?);");
			ps.setString(1, seq);
			ps.setString(2, strand.toAbbreviatedString());
			ps.setInt(3, start);
			ps.setInt(4, end);
			ps.setString(5, label);
			ps.setDouble(6, value);
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

	public int renameSequence(String oldName, String newName) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("update temp set sequence=? where sequence=?;");
			ps.setString(1, newName);
			ps.setString(2, oldName);
			return ps.executeUpdate();
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
	 * Check if sequence names in the imported features match our sequence names.
	 * @return a list of unknown sequence names, or any empty list if all sequence names are OK.
	 */
	public List<String> checkSequenceNames() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(connectString);
			ps = conn.prepareStatement("select sequence from temp where sequence not in (select distinct name from sequences);");
			rs = ps.executeQuery();
			List<String> unknownSequences = new ArrayList<String>();
			while (rs.next()) {
				unknownSequences.add(rs.getString(1));
			}
			return unknownSequences;
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
				log.warn("Error closing ResultSet", e1);
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
	 * copy from temp table into a table of quantitative segment features,
	 * sorting by sequence, strand, start, and end
	 */
	void copyAndSortToSeqmentFeatures(String table) {
		Connection conn = null;
		Statement s = null;
		try {
			conn = DriverManager.getConnection(connectString);
			s = conn.createStatement();
			s.executeUpdate(
					"create table " + table + " (" +
							"sequences_id integer NOT NULL, " +
							"strand text NOT NULL, " +
							"start integer NOT NULL, " +
							"end integer NOT NULL, " +
							"value numeric); ");
			s.executeUpdate(
					"insert into " + table
					+ " select s.id, t.strand, t.start, t.end, t.score as value "
					+ " from temp as t join sequences as s on t.sequence = s.name "
					+ " order by s.id, t.strand, t.start, t.end;");
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
	 * insert entry into tracks table
	 */
	void insertTrackEntry(String name, String type, String table) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = DriverManager.getConnection(connectString);

			// tracks (uuid, name, type, table_name, strand)
			ps = conn.prepareStatement("insert into tracks values(?, ?, ?, ?);");
			ps.setString(1, UUID.randomUUID().toString());
			ps.setString(2, name);
			ps.setString(3, type);
			ps.setString(4, table);
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

	public void createNewTrack(String name, String type, String table) {
		if (type.equals("quantitative.segment"))
			copyAndSortToSeqmentFeatures(table);
		else
			throw new RuntimeException("Unsupported track type: " + type);
		insertTrackEntry(name, type, table);
		dropTempTable();
	}

	


	/**
	 * process a line of a GFF file.
	 */
	class GffLineProcessor implements LineReader.LineProcessor {
		public void process(int lineNumber, String line) {
			// ignore comment lines
			if (line.startsWith("#")) return;

			// lines should have these fields:
			// seqname source feature start end score strand frame attributes comments
			// see: http://www.sanger.ac.uk/Software/formats/GFF/

			String[] fields = line.split("\t");
			System.out.println(Arrays.toString(fields));
			
			// create an entry in the temp table:
			// seq, strand, start, end, label, value
			insertFeatureIntoTemp(
					fields[0],
					Strand.fromString(fields[6]),
					Integer.parseInt(fields[3]),
					Integer.parseInt(fields[4]),
					fields[2],
					parseScore(fields[5])
			);
		}

		private double parseScore(String score) {
			if (score==null || "".equals(score))
				return -1.0;
			if (".".equals(score))
				return 0.0;
			try {
				return Double.parseDouble(score);
			}
			catch (Exception e) {
				return -1.0;
			}
		}
	}
}
