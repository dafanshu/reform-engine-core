package com.cloud.engine.delegate.event;

public interface ActivitiSequenceFlowTakenEvent extends ActivitiEvent {

	String getId();

	String getSourceActivityId();

	String getSourceActivityName();

	String getSourceActivityType();

	String getSourceActivityBehaviorClass();

	String getTargetActivityId();

	String getTargetActivityName();

	String getTargetActivityType();

	String getTargetActivityBehaviorClass();


}
