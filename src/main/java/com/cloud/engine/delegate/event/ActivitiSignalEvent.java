package com.cloud.engine.delegate.event;

public interface ActivitiSignalEvent extends ActivitiActivityEvent {

	/**
	 * @return the name of the signal. Returns null, if no specific signal name has been specified
	 * when signaling.
	 */
	public String getSignalName();

	/**
	 * @return the payload that was passed when signaling.
	 * Returns null, if no payload was passed.
	 */
	public Object getSignalData();

}
