package com.cloud.engine.delegate.event;

public interface ActivitiMessageEvent extends ActivitiActivityEvent {

	/**
	 * @return the name of the message.
	 */
	public String getMessageName();

	/**
	 * @return the payload that was passed
	 * when sending the message. Returns null, if no payload was passed.
	 */
	public Object getMessageData();

}
