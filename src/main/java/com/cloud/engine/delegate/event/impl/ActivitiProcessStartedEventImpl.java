package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiEventType;
import com.cloud.engine.delegate.event.ActivitiProcessStartedEvent;

import java.util.Map;

public class ActivitiProcessStartedEventImpl extends ActivitiEntityWithVariablesEventImpl implements ActivitiProcessStartedEvent {

  protected  String nestedProcessInstanceId;

  protected  String nestedProcessDefinitionId;

  @SuppressWarnings("rawtypes")
  public ActivitiProcessStartedEventImpl(final Object entity, final Map variables, final boolean localScope) {
    super(entity, variables, localScope, ActivitiEventType.PROCESS_STARTED);
  }

  @Override
  public String getNestedProcessInstanceId() {
    return this.nestedProcessInstanceId;
  }

  @Override
  public String getNestedProcessDefinitionId() {
    return this.nestedProcessDefinitionId;
  }

}
