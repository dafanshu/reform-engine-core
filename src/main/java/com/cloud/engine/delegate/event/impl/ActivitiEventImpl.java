package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.ActivitiIllegalArgumentException;
import com.cloud.engine.delegate.event.ActivitiEvent;
import com.cloud.engine.delegate.event.ActivitiEventType;

public class ActivitiEventImpl implements ActivitiEvent {

	protected ActivitiEventType type;
	protected String executionId;
	protected String processInstanceId;
	protected String processDefinitionId;

	/**
	 * Creates a new event implementation, not part of an execution context.
	 */
	public ActivitiEventImpl(ActivitiEventType type) {
		this(type, null, null, null);
	}

	/**
	 * Creates a new event implementation, part of an execution context.
	 */
	public ActivitiEventImpl(ActivitiEventType type, String executionId, String processInstanceId,
      String processDefinitionId) {
		if(type == null) {
			throw new ActivitiIllegalArgumentException("type is null");
		}
		this.type = type;
	  this.executionId = executionId;
	  this.processInstanceId = processInstanceId;
	  this.processDefinitionId = processDefinitionId;
  }

	@Override
    public ActivitiEventType getType() {
		return type;
	}

	public void setType(ActivitiEventType type) {
		this.type = type;
	}

	@Override
	public String getExecutionId() {
	  return executionId;
  }

	public void setExecutionId(String executionId) {
	  this.executionId = executionId;
  }

	@Override
	public String getProcessDefinitionId() {
	  return processDefinitionId;
  }

	public void setProcessDefinitionId(String processDefinitionId) {
	  this.processDefinitionId = processDefinitionId;
  }

	@Override
	public String getProcessInstanceId() {
	  return processInstanceId;
  }

	public void setProcessInstanceId(String processInstanceId) {
	  this.processInstanceId = processInstanceId;
  }

}
