package org.systemsbiology.genomebrowser.sqlite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.systemsbiology.genomebrowser.app.ProgressListener;
import org.systemsbiology.genomebrowser.app.ProgressListenerSupport;
import org.systemsbiology.genomebrowser.model.Sequence;
import org.systemsbiology.genomebrowser.model.SequenceFetcher;
import org.systemsbiology.genomebrowser.model.Strand;


/**
 * Read and write sequence into SQLite DB.
 */
public class SequenceDAO extends SqliteDb implements SequenceFetcher {
	private static final Logger log = Logger.getLogger(SequenceDAO.class);
	private String connectString;
	private SqliteDataSource dataSource;
	private ProgressListenerSupport progressListenerSupport = new ProgressListenerSupport();
	private static final int INSERTS_PER_TRANSACTION = 500;

	static void loadSqliteDriver() {
		try {
			Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public SequenceDAO(File file, SqliteDataSource dataSource) {
		this(getConnectStringForFile(file.getAbsolutePath()), dataSource);
	}

	public SequenceDAO(String connectString, SqliteDataSource dataSource) {
		loadSqliteDriver();
		this.connectString = connectString;
		this.dataSource = dataSource;
	}

	public static String getConnectStringForFile(String filename) {
		return "jdbc:sqlite:" + filename;
	}
	
	
	// TODO could this be made to work for a FASTA file with an entry for each
	// sequence in the genome? Or automagically download FASTA files from NCBI?
	
	// for halo NRC-1 refseq IDs are:
	// NC_002607
	// NC_002608
	// NC_001869
	
	// for halo R1:
	// NC_010364
	// NC_010366
	// NC_010369
	// NC_010368
	// NC_010367

	/**
	 * import FASTA to DB
	 */
	public void readFastaFile(File file, Sequence sequence) {
		Connection conn = null;
		PreparedStatement ps = null;
		int bases = 1;
		int lineCounter = 0;
		BufferedReader in = null;

		log.info("Reading fasta file: " + file.getName());

		// Chunking many inserts into a single transaction is a lot faster
		// on OS X. It totally breaks on Linux, so we check which OS we're on
		// and act accordingly.

		// The error we got on Ubuntu Linux was:

		/*
		2011-06-16 19:15:57,411 ERROR SequenceDAO: error importing fasta file
		java.sql.SQLException: cannot commit transaction - SQL statements in progress
		at org.sqlite.DB.execute(DB.java:275)
		at org.sqlite.Stmt.exec(Stmt.java:56)
		at org.sqlite.Stmt.execute(Stmt.java:83)
		at org.systemsbiology.genomebrowser.sqlite.SqliteDb.commitTransaction(SqliteDb.java:35)
		at org.systemsbiology.genomebrowser.sqlite.SequenceDAO.readFastaFile(SequenceDAO.java:127)
		at org.systemsbiology.genomebrowser.ui.ImportFastaDialog$ImportFastaRunnable.run(ImportFastaDialog.java:361)
		at java.lang.Thread.run(Thread.java:679)
		*/

		log.info("getProperty(\"os.name\") = " + System.getProperty("os.name"));
		boolean chunkInserts = !(System.getProperty("os.name").contains("Linux"));
		log.info("Using chunked inserts = " + chunkInserts);

		// query to insert a line of sequence into the table
		String sql = ("insert into bases " + "(sequence_id, start, end, sequence)" 
				+ " values(?, ?, ?, ? )");

		long millisStart = System.currentTimeMillis();
		progressListenerSupport.fireSetExpectedProgressEvent(sequence.getLength());

		try {
			conn = DriverManager.getConnection(connectString);

			// get sequence id to link sequence to sequences table
			int sequence_id = getSequenceId(conn, sequence.getUuid());

			if (!dataSource.tableExists(conn, "bases")) {
				createBasesTable(conn);
			}
			else {
				// if sequence for this sequence already exists in DB, remove it first
				clearExistingSequence(conn, sequence_id);
			}

			// create prepared statement to do inserts
			// ps gets closed in the finally clause
			ps = conn.prepareStatement(sql);
			
			log.info("begin transaction");
			if (chunkInserts) beginTransaction(conn);

			in = new BufferedReader(new FileReader(file));
			String line;
			while ( (line = in.readLine()) != null ) {

				if (line.contains(">") || (line.isEmpty()) ) {
					//do nothing
				}
				else {
					int start_position = bases;
					int end_position = bases + line.length() - 1;
					bases = end_position + 1;
					
					lineCounter++;
					if (lineCounter % INSERTS_PER_TRANSACTION == 0) {
						log.debug("loading sequence from fasta file: " + lineCounter + " lines, " + bases + " bases.");
						if (chunkInserts) {
							commitTransaction(conn);
							beginTransaction(conn);
						}
						progressListenerSupport.fireProgressEvent(bases);
				    }

					// send line to DB
					ps.setObject(1, sequence_id);
					ps.setObject(2, start_position);
				    ps.setObject(3, end_position);
				    ps.setObject(4, line);
				    ps.executeUpdate();

					//System.out.println("insert into bases " + "(sequence_id,start, end,sequence)" + " values(1, "+ start+", " +end+ "," + "'"+line+"'"+")");
				}
			}
			log.info("commit last transcation: " + lineCounter + " lines, " + bases + " bases.");
			if (chunkInserts) commitTransaction(conn);
			progressListenerSupport.fireDoneEvent();
			log.info("reading FASTA file took: " + (System.currentTimeMillis() - millisStart) / 1000.0);
		}
		catch (Exception e) {
			log.error("error importing fasta file", e);
			try {
				rollbackTransaction(conn);
			}
			catch (SQLException se) {
				log.error("Exception while rolling back transaction.", se);
			}
			throw new RuntimeException(e);
		}
		finally {
			if (in != null) {
				try {
					in.close(); //close file
				}
				catch (IOException e) {
					log.error(e);
				}
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
	//---- end import FASTA to DB

	private int getSequenceId(Connection conn, UUID sequence_uuid) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = conn.prepareStatement("select id from sequences where uuid=?;");
			ps.setObject(1, sequence_uuid);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
			throw new RuntimeException("No sequence found for uuid " + sequence_uuid.toString());
		}
		catch (SQLException e) {
			throw e;
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
		}
		
	}

	private void createBasesTable(Connection conn) throws SQLException {
		Statement s = null;
		try {
			s = conn.createStatement();
			s.executeUpdate("create table bases (sequence_id int, start int, end int, sequence text);");
		}
		finally {
			try {
				if (s != null)
					s.close();
			}
			catch (Exception e1) {
				log.warn("Error closing Statement", e1);
			}
		}
	}

	private void clearExistingSequence(Connection conn, int sequences_id) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("delete from bases where sequence_id=?;");
			ps.setInt(1, sequences_id);
			ps.executeUpdate();
		}
		finally {
			try {
				if (ps != null)
					ps.close();
			}
			catch (Exception e1) {
				log.warn("Error closing Statement", e1);
			}
		}
	}

	/**
	 * retrieve sequence for the given segment of the genome.
	 */
	public String getSequence(String sequenceName, Strand strand, int start, int end) {
		start = Math.max(start, 1);
		log.debug("getSequence(" + sequenceName + ", " + strand + ", " + start + ", " + end + ")");

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder sb = new StringBuilder();

		try {
			conn = DriverManager.getConnection(connectString);
			if (!dataSource.tableExists(conn, "bases")) {
				log.warn("No sequence data present");
				return "";
			}

			ps = conn.prepareStatement("select * from bases where end >= ? and start <= ? " + 
					"and sequence_id = (select id from sequences where name=?) order by start;");
			ps.setInt(1, start);
			ps.setInt(2, end);
			ps.setString(3, sequenceName);
			rs = ps.executeQuery();
			
			int resultStart = Integer.MAX_VALUE;
			int resultEnd = Integer.MIN_VALUE;
			
			while (rs.next()) {
				resultStart = Math.min(resultStart, rs.getInt(2));
				resultEnd = Math.max(resultEnd, rs.getInt(3));
				sb.append(rs.getString(4));
			}
//			String result = sb.substring(start - resultStart, end - resultStart + 1);
//			log.debug(result);
//			log.debug("Inv : " + InvertionUtils.inversion(result));
			
			
			if (sb.length() == 0){
				log.warn("sb.length() = 0");
				return "";
			}
			if (sb.length() < (end - start + 1)) {
				log.warn("sequence length found: " + sb.length());
				// return what we can, probably empty string
				return sb.substring(start - resultStart,sb.length());
			}
				String result = "";
				/*if (strand.toString().equals("reverse")){
					result =  InvertionUtils.inversion(sb.substring(start - resultStart, end - resultStart + 1));
				}
				else {
					result = sb.substring(start - resultStart, end - resultStart + 1);
				}*/
				result = sb.substring(start - resultStart, end - resultStart + 1);
				return result;	
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


	public void addProgressListener(ProgressListener listener) {
		progressListenerSupport.addProgressListener(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListenerSupport.removeProgressListener(listener);
	}
}
