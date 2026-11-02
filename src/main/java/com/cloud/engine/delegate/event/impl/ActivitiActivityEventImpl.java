package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.delegate.event.ActivitiActivityEvent;
import com.cloud.engine.delegate.event.ActivitiEventType;

public class ActivitiActivityEventImpl extends ActivitiEventImpl implements ActivitiActivityEvent {

	protected String activityId;
	protected String activityName;
	protected String activityType;
	protected String behaviorClass;

	public ActivitiActivityEventImpl(ActivitiEventType type) {
	  super(type);
  }

        @Override
    public String getActivityId() {
		return activityId;
        }

	public void setActivityId(String activityId) {
	  this.activityId = activityId;
  }

	@Override
	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	@Override
	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}

	@Override
	public String getBehaviorClass() {
		return behaviorClass;
	}

	public void setBehaviorClass(String behaviorClass) {
		this.behaviorClass = behaviorClass;
	}

}
