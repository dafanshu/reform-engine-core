package com.cloud.engine.pvm;

public class PvmException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PvmException(String message, Throwable cause) {
    super(message, cause);
  }

  public PvmException(String message) {
    super(message);
  }
}
