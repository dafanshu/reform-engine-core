package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiEventType;
import com.cloud.engine.delegate.event.ActivitiMembershipEvent;

public class ActivitiMembershipEventImpl extends ActivitiEventImpl implements ActivitiMembershipEvent {

	protected String userId;
	protected String groupId;

	public ActivitiMembershipEventImpl(ActivitiEventType type) {
	  super(type);
  }

	public void setUserId(String userId) {
	  this.userId = userId;
  }

	@Override
    public String getUserId() {
	  return userId;
  }

	public void setGroupId(String groupId) {
	  this.groupId = groupId;
  }

	@Override
	public String getGroupId() {
	  return groupId;
  }
}
