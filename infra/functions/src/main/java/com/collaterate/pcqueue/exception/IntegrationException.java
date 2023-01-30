package com.collaterate.pcqueue.exception;

public class IntegrationException extends RuntimeException {

  /** This is a custom exception which needs to be thrown to the calling method */
  private static final long serialVersionUID = 1L;

  public IntegrationException(String errorMessage, Throwable cause) {
    super(errorMessage, cause);
  }

  public IntegrationException(String message) {
    super(message);
  }
}
