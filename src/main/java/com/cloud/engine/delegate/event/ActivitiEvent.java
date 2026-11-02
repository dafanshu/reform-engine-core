package com.cloud.engine.delegate.event;

public interface ActivitiEvent {

	/**
	 * @return type of event.
	 */
	ActivitiEventType getType();

	/**
	 * @return the id of the execution this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getExecutionId();

	/**
	 * @return the id of the process instance this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getProcessInstanceId();

	/**
	 * @return the id of the process definition this event is associated with. Returns null, if the event
	 * was not dispatched from within an active execution.
	 */
	String getProcessDefinitionId();

}
