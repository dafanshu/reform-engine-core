package com.cloud.engine.delegate.event;

public interface ActivitiExceptionEvent {

	/**
	 * @return the throwable that caused this event to be dispatched.
	 */
	Throwable getCause();
}
