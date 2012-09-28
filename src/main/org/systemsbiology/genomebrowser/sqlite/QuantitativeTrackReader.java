package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.io.LineReader;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * Read a TSV file with three columns (start, end, value) into a table.
 * @author cbare
 */
public class QuantitativeTrackReader implements LineReader.LineProcessor {
	private static final Logger log = Logger.getLogger(QuantitativeTrackReader.class);
	private Connection conn;
	private Statement stmnt;
	private PreparedStatement ps;
	private String seqId;
	private Strand strand;
	private String table;
	private String filename;
	private int count;


	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}

	public void setStrand(Strand strand) {
		this.strand = strand;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public void setDbFilename(String filename) {
		this.filename = filename;
	}

	public void read(String path) throws Exception {
		Class.forName("org.sqlite.JDBC");
		
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
			stmnt = conn.createStatement();
			stmnt.executeUpdate("drop table if exists " + table + ";");
			stmnt.executeUpdate("create table " + table + " (seqId, strand, start, end, value);");
			ps = conn.prepareStatement("insert into " + table + " values (?,?,?,?,?);");

			LineReader loader = new LineReader(this);
			loader.loadData(path);
			if (count > 0)
				executeBatch();
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

	public void process(int lineNumber, String line) {
		System.out.println(String.format("%04d: %s", lineNumber, line));
		if (lineNumber > 0) {
			String[] fields = line.split("\t");

			int start = Integer.parseInt(fields[0]);
			int end = Integer.parseInt(fields[1]);
			double value = Double.parseDouble(fields[2]);

			try {
				ps.setString(1, seqId);
				ps.setString(2, strand.toAbbreviatedString());
				ps.setInt(3, start);
				ps.setInt(4, end);
				ps.setDouble(5, value);
				ps.addBatch();
				
				count++;
				if (count >= 100) {
					executeBatch();
				}
			}
			catch (SQLException e) {
				throw new RuntimeException("Error writing to DB",e);
			}
		}
	}

	private void executeBatch() throws SQLException {
		conn.setAutoCommit(false);
		ps.executeBatch();
		conn.commit();
		conn.setAutoCommit(true);
		count = 0;
	}
}
