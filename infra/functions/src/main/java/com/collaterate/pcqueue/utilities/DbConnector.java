/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DbConnector {

	public static Connection getConnection(Map<String, String> dbConnectionSecret)
			throws SQLException {
		return DriverManager.getConnection(
				getDatabaseUrl(dbConnectionSecret),
				dbConnectionSecret.get("username"),
				dbConnectionSecret.get("password"));
	}

	private static String getDatabaseUrl(Map<String, String> dbConnectionSecret) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder
				.append("jdbc:postgresql://")
				.append(dbConnectionSecret.get("host"))
				.append("/")
				.append(dbConnectionSecret.get("dbname"));
		return stringBuilder.toString();

	}
}
