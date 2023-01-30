/*******************************************************************************
 * Copyright 2023 Collaterate. All rights reserved.
 *******************************************************************************/

package com.collaterate.pcqueue.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.collaterate.pcqueue.exception.FileResourceException;

public class FileResourcesUtils {

	public String getFileResource(String fileName) throws FileResourceException, IOException {

		InputStream is = getFileFromResourceAsStream(fileName);
		try {
			return printInputStream(is);
		} catch (Exception exception) {
			throw new FileResourceException(
					"Encountered exception while reading from resource folder : {}", exception);
		}
	}

	private InputStream getFileFromResourceAsStream(String fileName) throws IllegalArgumentException {

		// The class loader that loaded the class
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);

		// the stream holding the file content
		if (inputStream == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {
			return inputStream;
		}
	}

	private String printInputStream(InputStream is) throws IOException {

		String strLine = null;
		InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		StringBuilder resultString = new StringBuilder();

		while ((strLine = reader.readLine()) != null) {
			resultString = resultString.append(strLine.trim() + " ");
		}
		reader.close();
		streamReader.close();
		return resultString.toString();
	}
}
