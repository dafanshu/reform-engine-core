package com.cloud.engine.delegate.event;

public interface ActivitiActivityEvent extends ActivitiEvent {

	/**
	 * @return the id of the activity this event is related to. This corresponds to an
	 * id defined in the process definition.
	 */
	String getActivityId();

	/**
	 * @return the name of the activity this event is related to.
	 */
	String getActivityName();

	/**
	 * @return the type of the activity (if set during parsing).
	 */
	String getActivityType();

	/**
	 * @return the behaviourclass of the activity (if it could be determined)
	 */
	String getBehaviorClass();

}
