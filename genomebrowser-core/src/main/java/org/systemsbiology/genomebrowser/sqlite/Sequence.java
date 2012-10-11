package org.systemsbiology.genomebrowser.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;


public class Sequence {
	private static final Logger log = Logger.getLogger(SqliteDb.class);
	private String connectString;

	static void loadSqliteDriver() {
		try {
			Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Sequence(File file) {
		this(getConnectStringForFile(file.getAbsolutePath()));
	}

	public Sequence(String connectString) {
		loadSqliteDriver();
		this.connectString = connectString;
	}

	public static String getConnectStringForFile(String filename) {
		return "jdbc:sqlite:" + filename;
	}

	public String getSequence(String sequenceName, int start, int end) {
		log.debug("getSequence()");

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder sb = new StringBuilder();

		try {
			conn = DriverManager.getConnection(connectString);

			ps = conn.prepareStatement("select * from sequence where end >= ? and start <= ? order by start;");
			ps.setInt(1, start);
			ps.setInt(2, end);
			rs = ps.executeQuery();
			
			int resultStart = Integer.MAX_VALUE;
			int resultEnd = Integer.MIN_VALUE;
			
			while (rs.next()) {
				resultStart = Math.min(resultStart, rs.getInt(2));
				resultEnd = Math.max(resultEnd, rs.getInt(3));
				sb.append(rs.getString(4));
			}
			String seq = sb.toString();
			
			return seq.substring(start - resultStart, end - resultStart + 1);
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


	
	public static void main(String[] args) {
		log.info("Getting sequence...");
		Map<String, String> dbInfo = SqliteDataSource.getDatabaseInfo();
		System.out.println(dbInfo);
		String filename = args[0];
		System.out.println("filename=" + filename);
		Sequence seq = new Sequence(new File(filename));
		
		String s = seq.getSequence("fred", 772, 842);
		System.out.println(s);
		
//		String expected = "CGATCAAGTCCGGCGAGTTGCAGGTCGTGTCCGATGGGCGCATCGTCGAGCGGGCGCCAGTCGCCAACGT" +
//		"TTCGGAAAGCGACAGTGCGAACGTTACCTTCGATGGGGCGTCGATCCCCAGCGGCGAGTTAGTGATCCGC" +
//		"GGCGAGTACACCCTCGACGACGAACACAGCACGCACACCACGAACACGACACTCACCTACCAACCACAGC" +
//		"GCTCCGCAGACGTTGCGCTCACTGGTGTTGAGGCATCAGGTGGGGGGACCACGTACACGATCAGCGGCGA";
//
//		System.out.println(expected.equals(s));
		
		String expected = "GATCAAGTCCGGCGAGTTGCAGGTCGTGTCCGATGGGCGCATCGTCGAGCGGGCGCCAGTCGCCAACGTTT";
		System.out.println(expected.equals(s));
	}

}
