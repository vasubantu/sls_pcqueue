package com.collaterate.pcqueue.exception;

public class CollaterateLambdaException extends RuntimeException {

  /** This is a custom exception which needs to be thrown to the calling method */
  private static final long serialVersionUID = 1L;

  public CollaterateLambdaException(String errorMessage, Throwable cause) {
    super(errorMessage, cause);
  }

  public CollaterateLambdaException(String errorMessage) {
    super(errorMessage);
  }
}
