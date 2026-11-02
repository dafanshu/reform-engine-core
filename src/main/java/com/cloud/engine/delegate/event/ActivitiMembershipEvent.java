package com.cloud.engine.delegate.event;

public interface ActivitiMembershipEvent extends ActivitiEvent {

	/**
	 * @return related user. Returns null, if not related to a sigle user but rather to all
	 * members of the group.
	 */
	String getUserId();

	/**
	 * @return related group
	 */
	String getGroupId();
}
