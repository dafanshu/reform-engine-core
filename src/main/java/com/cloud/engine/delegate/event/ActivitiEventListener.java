package com.cloud.engine.delegate.event;

public interface ActivitiEventListener {

	/**
	 * Called when an event has been fired
	 * @param event the event
	 */
	void onEvent(ActivitiEvent event);

	/**
	 * @return whether or not the current operation should fail when this listeners execution
	 * throws an exception.
	 */
	boolean isFailOnException();
}
