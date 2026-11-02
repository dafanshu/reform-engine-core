package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationTransitionNotifyListenerEnd extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationTransitionNotifyListenerEnd.class);

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    execution.performOperation(TRANSITION_DESTROY_SCOPE);
    log.debug("{} perform {}: {}", execution, this, execution.getActivity());
  }

  @Override
  public String toString() {
    return "transition_notify_listener_end";
  }
}
