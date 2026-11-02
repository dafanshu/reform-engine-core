package com.cloud.engine;

public class ActivitiObjectNotFoundException extends ActivitiException {

  private static final long serialVersionUID = 1L;

  private Class<?> objectClass;

  public ActivitiObjectNotFoundException(String message) {
    super(message);
  }

  public ActivitiObjectNotFoundException(String message, Class<?> objectClass) {
    this(message, objectClass, null);
  }

  public ActivitiObjectNotFoundException(Class<?> objectClass) {
    this(null, objectClass, null);
  }

  public ActivitiObjectNotFoundException(String message, Class<?> objectClass, Throwable cause) {
    super(message, cause);
    this.objectClass = objectClass;
  }

  /**
   * The class of the object that was not found. Contains the
   * interface-class of the activiti-object that was not found.
   */
  public Class<?> getObjectClass() {
    return objectClass;
  }
}
