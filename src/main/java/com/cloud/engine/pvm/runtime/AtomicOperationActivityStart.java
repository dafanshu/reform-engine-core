package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationActivityStart extends AbstractEventAtomicOperation {
  private static Logger log = LoggerFactory.getLogger(AtomicOperationActivityStart.class);
  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    execution.performOperation(AtomicOperation.ACTIVITY_EXECUTE);
    log.debug("{} perform {}: {}", execution, this, execution.getActivity());
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_START;
  }

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  public String toString() {
    return "activity_start";
  }
}
