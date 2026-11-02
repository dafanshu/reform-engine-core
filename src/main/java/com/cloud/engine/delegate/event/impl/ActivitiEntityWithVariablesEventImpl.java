package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiEntityWithVariablesEvent;
import com.cloud.engine.delegate.event.ActivitiEventType;
import java.util.Map;

public class ActivitiEntityWithVariablesEventImpl extends ActivitiEntityEventImpl implements ActivitiEntityWithVariablesEvent {

  protected Map variables;
  protected boolean localScope;

  public ActivitiEntityWithVariablesEventImpl(Object entity, Map variables, boolean localScope, ActivitiEventType type) {
		super(entity, type);

		this.variables = variables;
		this.localScope = localScope;
  }

  @Override
  public Map getVariables() {
    return variables;
  }

  @Override
  public boolean isLocalScope() {
    return localScope;
  }
}
