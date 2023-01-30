/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.pesistence;

import java.sql.Connection;

import com.collaterate.pcqueue.exception.IntegrationException;

public abstract class BaseDao {

	protected BaseDao() {
	}

	protected static void verifyConnection(Connection connection) {
		if (connection == null) {
			throw new IntegrationException("database connection is null");
		}
	}
}
