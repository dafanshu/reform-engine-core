package com.cloud.engine.delegate.event;

public interface ActivitiEntityEvent extends ActivitiEvent {

	/**
	 * @return the entity that is targeted by this event.
	 */
	Object getEntity();
}
