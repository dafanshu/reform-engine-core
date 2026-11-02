package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import com.cloud.engine.pvm.process.TransitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationTransitionNotifyListenerStart extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationTransitionNotifyListenerStart.class);

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_START;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    TransitionImpl transition = execution.getTransition();
    ActivityImpl destination = null;
    // this is null after async cont. -> transition is not stored in execution
    if(transition == null) {
      destination = (ActivityImpl) execution.getActivity();
    } else {
      destination = transition.getDestination();
    }
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if (activity!=destination) {
      ActivityImpl nextScope = AtomicOperationTransitionNotifyListenerTake.findNextScope(activity, destination);
      execution.setActivity(nextScope);
      execution.performOperation(TRANSITION_CREATE_SCOPE);
    } else {
      execution.setTransition(null);
      execution.setActivity(destination);
      execution.performOperation(ACTIVITY_EXECUTE);
    }
    log.debug("{} perform {}: {}", execution, this, activity);
  }

  @Override
  public String toString() {
    return "transition_notify_listener_start";
  }
}
