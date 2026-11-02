package com.cloud.engine.delegate.event;

public interface ActivitiCancelledEvent extends ActivitiEvent{
  /**
   * @return the cause of the cancel event. Returns null, if no specific cause has been specified.
   */
  public Object getCause();
}
