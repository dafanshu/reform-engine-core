package com.cloud.engine.delegate.event;

public interface ActivitiProcessStartedEvent extends ActivitiEntityWithVariablesEvent {

  /**
   * @return the id of the process instance of the nested process that starts the current process instance, or null if
   *         the current process instance is not started into a nested process.
   */
  String getNestedProcessInstanceId();

  /**
   * @return the id of the process definition of the nested process that starts the current process instance, or null
   *         if the current process instance is not started into a nested process.
   */
  String getNestedProcessDefinitionId();
}
