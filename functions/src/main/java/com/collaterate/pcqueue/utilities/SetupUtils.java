/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.utilities;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.collaterate.pcqueue.exception.IntegrationException;
import com.google.gson.Gson;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SetupUtils {

	private static final Logger LOGGER = LogManager.getLogger(SetupUtils.class);

	public static Connection setupConnection(String dbSecretsKey) {
		Map<String, String> dbCredentialsMap = new Gson().fromJson(AWSUtils.getSecret(dbSecretsKey), Map.class);
		try {
			return DbConnector.getConnection(dbCredentialsMap);
		} catch (SQLException e) {
			LOGGER.error("Unable to connect to the database: with key {}", dbSecretsKey, e);
			throw new IntegrationException(e.getMessage(), e);
		}
	}
}
