package com.cloud.engine.delegate.event.impl;

import com.cloud.engine.ActivitiIllegalArgumentException;
import com.cloud.engine.delegate.event.ActivitiEntityEvent;
import com.cloud.engine.delegate.event.ActivitiEventType;

public class ActivitiEntityEventImpl extends ActivitiEventImpl implements ActivitiEntityEvent {

	protected Object entity;

	public ActivitiEntityEventImpl(Object entity, ActivitiEventType type) {
		super(type);
		if(entity == null) {
			throw new ActivitiIllegalArgumentException("Entity cannot be null.");
		}
	  this.entity = entity;
        }

        @Override
	public Object getEntity() {
		return entity;
	}
}
