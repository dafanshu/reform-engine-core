package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiEventType;
import com.cloud.engine.delegate.event.ActivitiSignalEvent;

public class ActivitiSignalEventImpl extends ActivitiActivityEventImpl implements ActivitiSignalEvent {

	protected String signalName;
	protected Object signalData;

	public ActivitiSignalEventImpl(ActivitiEventType type) {
	  super(type);
  }

        @Override
    public String getSignalName() {
		return signalName;
  }

	public void setSignalName(String signalName) {
	  this.signalName = signalName;
  }

        @Override
	public Object getSignalData() {
		return signalData;
  }

	public void setSignalData(Object signalData) {
	  this.signalData = signalData;
  }
}
