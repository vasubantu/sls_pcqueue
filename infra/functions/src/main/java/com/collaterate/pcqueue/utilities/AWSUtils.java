/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.utilities;

import java.util.Base64;

import com.collaterate.pcqueue.exception.IntegrationException;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class AWSUtils {

	private AWSUtils() {
		throw new AssertionError("Instantiating utility class.");
	}

	private static final String REGION = System.getenv("REGION");

	public static String getSecret(String secretName) {

		// Create a Secrets Manager client
		SecretsManagerClient client = SecretsManagerClient.builder()
				.region(Region.of(REGION))
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();

		GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
		GetSecretValueResponse getSecretValueResponse = null;

		try {
			getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
		} catch (Exception e) {
			throw new IntegrationException("Unable to gain access to Secret: " + secretName, e);
		}
		// Decrypts secret using the associated KMS key.
		// Depending on whether the secret is a string or binary, one of these fields
		// will be populated.
		if (getSecretValueResponse.secretString() != null) {
			return getSecretValueResponse.secretString();
		}
		return new String(
				Base64.getDecoder().decode(getSecretValueResponse.secretBinary().asByteBuffer()).array());
	}
}
