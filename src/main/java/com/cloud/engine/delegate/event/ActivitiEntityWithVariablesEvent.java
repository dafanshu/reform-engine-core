package com.cloud.engine.delegate.event;

import java.util.Map;

public interface ActivitiEntityWithVariablesEvent extends ActivitiEntityEvent {

	/**
	 * @return the variables created together with the entity.
	 */
        Map getVariables();

	boolean isLocalScope();
}
