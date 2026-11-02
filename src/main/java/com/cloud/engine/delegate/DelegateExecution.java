package com.cloud.engine.delegate;

public interface DelegateExecution extends VariableScope {

  /** execution id */
  String getId();

  /** 流程实例ID */
  String getProcessInstanceId();

  /** 监听的事件名称 */
  String getEventName();

  /**
   * The process definition key for the process instance this execution is associated with.
   */
  String getProcessDefinitionId();

  /**
   * Gets the id of the parent of this execution. If null, the execution represents a process-instance.
   */
  String getParentId();

  /**
   * Gets the id of the calling execution. If not null, the execution is part of a subprocess.
   */
  String getSuperExecutionId();

  /**
   * Gets the id of the current activity.
   */
  String getCurrentActivityId();

  /**
   * Gets the name of the current activity.
   */
  String getCurrentActivityName();

  /**
   * Returns the tenant id, if any is set before on the process definition or process instance.
   */
  String getTenantId();

}
