package org.systemsbiology.genomebrowser.sqlite;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public abstract class SqliteDb {
	private static final Logger log = Logger.getLogger(SqliteDb.class);

	protected void beginTransaction(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("begin transaction;");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while beginning transaction", e);
			}
		}
	}

	protected void commitTransaction(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("commit;");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while committing transaction", e);
			}
		}
	}

	protected void rollbackTransaction(Connection conn) throws SQLException {
		Statement statement = null;
		try {
			statement = conn.createStatement();
			statement.execute("rollback;");
		}
		finally {
			try {
				if (statement != null)
					statement.close();
			}
			catch (Exception e) {
				log.error("Exception while rolling back transaction", e);
			}
		}
	}

	/**
	 * return true if table exists, false otherwise
	 * @throws SQLException
	 */
	public boolean tableExists(Connection conn, String name) throws SQLException {
		ResultSet rs = null;
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			rs = dbmd.getTables(null, null, "name", null);
			// return true if table exists
			return rs.next();
		}
		finally {
			try {
				if (rs != null)
					rs.close();
			}
			catch (Exception e) {
				log.error("Exception in tableExists", e);
			}
		}
	}

	public boolean tableHasColumn(Connection conn, String tableName, String columnName) throws SQLException {
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet columns = meta.getColumns(null, null, tableName, columnName);
		if (columns.next()) {
		  log.info("found column: " + columns.getString("COLUMN_NAME"));
		  return true;
		}
		return false;
	}
}
