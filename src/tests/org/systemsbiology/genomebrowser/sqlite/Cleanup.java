package org.systemsbiology.genomebrowser.sqlite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Cleanup {

	public void cleanup() throws Exception {
		String sql = "select uuid, table_name from tracks where uuid not in (" +
					"'b7c82f8e-1485-13a6-56a0-5454028fbe19', " + 
					"'b7c82f9e-1485-13a6-5683-98d8f23b06e0', " + 
					"'b7c82fad-1485-13a6-5636-cb652db72455', " + 
					"'b7e980f9-1485-13a6-5615-f47620241891', " + 
					"'b7e98109-1485-13a6-5642-8d5c66376f66', " + 
					"'b7e98119-1485-13a6-56d4-15206fb3a1eb', " + 
					"'b7e9ab26-1485-13a6-56ef-7b947915f1e8')";

		String connect = "jdbc:sqlite:test.hbgb";
		Class.forName("org.sqlite.JDBC");

		Connection conn = DriverManager.getConnection(connect);
		Statement st = conn.createStatement();
		
		// get track UUIDs and tables to delete
		ResultSet rs = st.executeQuery(sql);
		List<String> uuids = new ArrayList<String>();
		List<String> tables = new ArrayList<String>();
		while (rs.next()) {
			uuids.add(rs.getString(1));
			tables.add(rs.getString(2));
		}
		rs.close();
		
		for (String uuid : uuids) {
			sql = String.format("delete from tracks where uuid='%s';", uuid);
			st.execute(sql);
		}
		
		for (String uuid : uuids) {
			sql = String.format("delete from datasets_tracks where tracks_uuid='%s';", uuid);
			st.execute(sql);
		}

		for (String uuid : uuids) {
			sql = String.format("delete from attributes where uuid='%s';", uuid);
			st.execute(sql);
		}

		for (String uuid : uuids) {
			sql = String.format("delete from block_index where tracks_uuid='%s';", uuid);
			st.execute(sql);
		}

		for (String table : tables) {
			sql = String.format("drop table %s;", table);
			st.execute(sql);
		}

		// find and drop coordinate map tables
		tables.clear();
		DatabaseMetaData dbmd = conn.getMetaData();
		rs = dbmd.getTables(null, null, "map_%", null);
		while (rs.next()) {
			tables.add(rs.getString(3));
		}
		rs.close();

		for (String table : tables) {
			sql = String.format("drop table %s;", table);
			st.executeUpdate(sql);
		}

		st.close();
		conn.close();
		
		System.out.println("database test.hbgb cleaned!");
	}

	public static void main(String[] args) throws Exception {
		new Cleanup().cleanup();
	}
}
