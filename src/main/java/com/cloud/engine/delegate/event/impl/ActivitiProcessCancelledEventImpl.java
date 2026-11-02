package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiCancelledEvent;
import com.cloud.engine.delegate.event.ActivitiEventType;

public class ActivitiProcessCancelledEventImpl extends ActivitiEventImpl implements ActivitiCancelledEvent {

	protected Object cause;

	public ActivitiProcessCancelledEventImpl() {
	  super(ActivitiEventType.PROCESS_CANCELLED);
  }

	public void setCause(Object cause) {
	  this.cause = cause;
  }

	@Override
	public Object getCause() {
	  return cause;
  }

}
