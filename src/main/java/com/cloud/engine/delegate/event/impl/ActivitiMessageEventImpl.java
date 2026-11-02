package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiEventType;
import com.cloud.engine.delegate.event.ActivitiMessageEvent;

public class ActivitiMessageEventImpl extends ActivitiActivityEventImpl implements ActivitiMessageEvent {

	protected String messageName;
	protected Object messageData;

	public ActivitiMessageEventImpl(ActivitiEventType type) {
	  super(type);
  }

	public void setMessageName(String messageName) {
	  this.messageName = messageName;
  }

	@Override
	public String getMessageName() {
	  return messageName;
  }

	public void setMessageData(Object messageData) {
	  this.messageData = messageData;
  }

	@Override
	public Object getMessageData() {
	  return messageData;
  }
}
