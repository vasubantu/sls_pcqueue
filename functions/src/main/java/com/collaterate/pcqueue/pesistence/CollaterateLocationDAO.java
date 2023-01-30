/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.pesistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.collaterate.pcqueue.exception.CollaterateLambdaDAOException;
import com.collaterate.pcqueue.exception.FileResourceException;
import com.collaterate.pcqueue.utilities.FileResourcesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Vasu Bantu */
public class CollaterateLocationDAO extends BaseDao {

	private static final FileResourcesUtils fileResourcesUtils = new FileResourcesUtils();
	private static final Logger LOGGER = LoggerFactory.getLogger(CollaterateLocationDAO.class);

	/**
	 * This method returns the current batch number.
	 *
	 * @return batchNumber
	 */
	public static ArrayList<CollaterateLocation> getLocationTable(Connection dbConnection) {

		ResultSet locationTableResultSet = null;
		String locationTable = null;
		PreparedStatement getLocationTablePreparedStatement = null;
		ArrayList<CollaterateLocation> li = new ArrayList<>();
		// final Request r = new Request();

		try {
			verifyConnection(dbConnection);
			String fileName = "getLocationTable.sql";
			String locationTableQuery = fileResourcesUtils.getFileResource(fileName);
			LOGGER.info("Location table query: {}", locationTableQuery);
			getLocationTablePreparedStatement = dbConnection.prepareStatement(locationTableQuery);

			locationTableResultSet = getLocationTablePreparedStatement.executeQuery();
			while (locationTableResultSet.next()) {
				final CollaterateLocation cl = new CollaterateLocation();
				cl.setId(locationTableResultSet.getInt(1));
				cl.setName(locationTableResultSet.getString(2));
				cl.setCode(locationTableResultSet.getString(3));
				cl.setDescription(locationTableResultSet.getString(4));
				li.add(cl);
			}
			LOGGER.info("Data retrieved is : {}", locationTable);
		} catch (SQLException sqlException) {
			throw new CollaterateLambdaDAOException(
					"Encountered sql exception in getting the Data", sqlException);
		} catch (FileResourceException | IOException | NullPointerException ex) {
			throw new CollaterateLambdaDAOException(
					"Encountered exception in getting the Data", ex);
		} finally {
			if (getLocationTablePreparedStatement != null) {
				try {
					getLocationTablePreparedStatement.close();
				} catch (Exception e) {
					LOGGER.info("Exception while closing the prepared statement: {}", e.getMessage(), e);
				}
			}
		}
		return li;
	}
}
