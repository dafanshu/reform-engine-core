package com.cloud.engine.delegate.event;

public interface ActivitiErrorEvent extends ActivitiActivityEvent {

	/**
	 * @return the error-code of the error. Returns null, if no specific error-code has been specified
	 * when the error was thrown.
	 */
	public String getErrorCode();
}
