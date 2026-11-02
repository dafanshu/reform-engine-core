package com.cloud.engine;


/**
 * Runtime exception that is the superclass of all Activiti exceptions.
 *
 * @author Tom Baeyens
 */
public class ActivitiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ActivitiException(String message, Throwable cause) {
    super(message, cause);
  }

  public ActivitiException(String message) {
    super(message);
  }
}
