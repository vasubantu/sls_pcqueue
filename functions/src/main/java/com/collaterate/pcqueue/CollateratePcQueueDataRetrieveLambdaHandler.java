/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.collaterate.pcqueue.exception.CollaterateLambdaDAOException;
import com.collaterate.pcqueue.exception.CollaterateLambdaException;
import com.collaterate.pcqueue.pesistence.CollaterateLocation;
import com.collaterate.pcqueue.pesistence.CollaterateLocationDAO;
import com.collaterate.pcqueue.utilities.SetupUtils;

/** @author Vasu Bantu */
public class CollateratePcQueueDataRetrieveLambdaHandler {

	private final Connection readerDbConnection;
	private static final Logger LOGGER = LoggerFactory.getLogger(CollateratePcQueueDataRetrieveLambdaHandler.class);
	private DynamoDB dynamoDB;

	public CollateratePcQueueDataRetrieveLambdaHandler() {

		readerDbConnection = SetupUtils.setupConnection(System.getenv("SECRET_ARN_READER"));
	}

	public CollateratePcQueueDataRetrieveLambdaHandler(
			Connection readerDbConnection) {
		this.readerDbConnection = readerDbConnection;
	}

	public String handleRequest() {
		try {
			List<CollaterateLocation> locationData = CollaterateLocationDAO.getLocationTable(readerDbConnection);

			AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
			dynamoDB = new DynamoDB(client);
			Table table = dynamoDB.getTable("collaterate_pcqueue_delta");
			PutItemOutcome outcome = null;
			for (CollaterateLocation locationData1 : locationData) {
				outcome = table.putItem(new Item().withPrimaryKey("id", locationData1.getId())
						.with("name", locationData1.getName())
						.with("code", locationData1.getCode())
						.with("description", locationData1.getDescription()));
			}
			if (Objects.nonNull(outcome))
				return "SUCCESS";
			else
				return "FAILURE";

		} catch (CollaterateLambdaDAOException re) {
			throw new CollaterateLambdaException(
					"Exception caught in retrieving or Dataaaa", re);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
}
