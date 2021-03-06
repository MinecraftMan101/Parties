package com.alessiodp.parties.storage.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alessiodp.parties.Parties;
import com.alessiodp.parties.addons.libraries.ILibrary;
import com.alessiodp.parties.configuration.Constants;
import com.alessiodp.parties.configuration.data.ConfigMain;
import com.alessiodp.parties.logging.LoggerManager;
import com.alessiodp.parties.logging.LogLevel;
import com.alessiodp.parties.storage.StorageType;
import com.alessiodp.parties.storage.interfaces.IDatabaseSQL;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLDao implements IDatabaseSQL {
	private Parties plugin;
	
	private HikariDataSource hikariDataSource;
	private boolean failed;
	
	public MySQLDao(Parties instance) {
		plugin = instance;
	}
	
	/*
	 * Connection
	 */
	@Override
	public void initSQL() {
		LoggerManager.log(LogLevel.DEBUG, Constants.CLASS_INIT.replace("{class}", getClass().getSimpleName()), true);
		failed = false;
		if (plugin.getLibraryManager().initLibrary(ILibrary.HIKARI)
				&& plugin.getLibraryManager().initLibrary(ILibrary.SLF4J_API)
				&& plugin.getLibraryManager().initLibrary(ILibrary.SLF4J_SIMPLE)) {
			try {
				initConnection();
			} catch (Exception ex) {
				LoggerManager.printError(Constants.DEBUG_SQL_FAILED
						.replace("{type}", StorageType.MYSQL.getFormattedName())
						.replace("{message}", ex.getMessage()));
				failed = true;
			}
		} else
			failed = true;
	}
	private void initConnection() throws Exception {
		HikariConfig config = new HikariConfig();
		config.setPoolName("Parties");
		config.setJdbcUrl("jdbc:mysql://"
				+ ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_ADDRESS
				+ "/" + ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_DATABASE);
		config.setUsername(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_USERNAME);
		config.setPassword(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_PASSWORD);
		config.setMaximumPoolSize(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_POOLSIZE);
		config.setMinimumIdle(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_POOLSIZE);
		config.setMaxLifetime(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_CONNLIFETIME);
		config.setIdleTimeout(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_CONNTIMEOUT);
		
		// Properties: https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html
		config.addDataSourceProperty("cachePreStmts", "true"); // Enable Prepared Statement caching
		config.addDataSourceProperty("prepStmtCacheSize", "25"); // How many PS cache, default: 25
		config.addDataSourceProperty("useServerPrepStmts", "true"); // If supported use PS server-side
		config.addDataSourceProperty("useLocalSessionState", "true"); // Enable setAutoCommit
		config.addDataSourceProperty("useLocalTransactionState", "true"); // Enable commit/rollbacks
		config.addDataSourceProperty("allowMultiQueries", "true"); // Support multiple queries, used to create tables
		config.addDataSourceProperty("useUnicode", "true"); // Forcing the use of unicode
		config.addDataSourceProperty("characterEncoding", "utf8"); // Setup encoding to UTF-8
		config.addDataSourceProperty("useSSL", Boolean.toString(ConfigMain.STORAGE_SETTINGS_SQL_MYSQL_USESSL));
		
		hikariDataSource = new HikariDataSource(config);
	}
	
	@Override
	public Connection getConnection() {
		Connection ret = null;
		try {
			ret = hikariDataSource.getConnection();
		} catch (Exception ex) {
			LoggerManager.printError(Constants.DEBUG_SQL_CONNECTIONERROR
					.replace("{storage}", StorageType.MYSQL.getFormattedName())
					.replace("{message}", ex.getMessage()));
		}
		return ret;
	}
	
	@Override
	public void stopSQL() {
		if (hikariDataSource != null && !hikariDataSource.isClosed()) {
			hikariDataSource.close();
		}
	}
	
	@Override
	public boolean isFailed() {
		return failed;
	}
	
	@Override
	public void handleSchema(HashMap<SQLTable, String> schema) {
		LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_SQL_SCHEMA_INIT.replace("{class}", getClass().getSimpleName()), true);
		BufferedReader buff = new BufferedReader(new InputStreamReader(
				plugin.getResource("schemas/" + Constants.DATABASE_MYSQL_SCHEMA),
				StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			while ((line = buff.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Matcher m = Pattern
				.compile(Constants.DATABASE_SCHEMA_DIVIDER, Pattern.CASE_INSENSITIVE)
				.matcher(sb.toString());
		while (m.find()) {
			LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_SQL_SCHEMA_FOUND.replace("{schema}", m.group(1)), true);
			SQLTable table = SQLTable.getExactEnum(m.group(1));
			if (table != null)
				schema.put(table, m.group(2));
		}
	}
}
