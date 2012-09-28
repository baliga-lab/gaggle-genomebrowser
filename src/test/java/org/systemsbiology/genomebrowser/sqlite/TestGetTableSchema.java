package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.junit.Test;


public class TestGetTableSchema {

	@Test
	public void test() throws Exception {
		String connect = "jdbc:sqlite:test.hbgb";
		Class.forName("org.sqlite.JDBC");

		Connection conn = null;
		
        conn = DriverManager.getConnection(connect);
		DatabaseMetaData dbmd = conn.getMetaData();

		ResultSet rs = dbmd.getTables(null, null, null, null);
		int cols = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			System.out.println(rs.getString(3).toLowerCase());
		}
		rs.close();

		rs = dbmd.getColumns(null, null, "features_matrix", "value%");
		cols = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			StringBuilder sb = new StringBuilder();
			for (int i=1; i<=cols; i++) {
				sb.append(rs.getString(i)).append(", ");
			}
			System.out.println(sb.toString());
		}
		rs.close();
		conn.close();
	}
}
