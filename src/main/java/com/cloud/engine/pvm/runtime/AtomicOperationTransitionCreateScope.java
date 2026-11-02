package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationTransitionCreateScope implements AtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationTransitionCreateScope.class);

  @Override
  public boolean isAsync(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    return activity.isAsync();
  }

  @Override
  public void execute(InterpretableExecution execution) {
    InterpretableExecution propagatingExecution = null;
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if (activity.isScope()) {
      propagatingExecution = (InterpretableExecution) execution.createExecution();
      propagatingExecution.setActivity(activity);
      propagatingExecution.setTransition(execution.getTransition());
      execution.setTransition(null);
      execution.setActivity(null);
      execution.setActive(false);
      log.debug("create scope: parent {} continues as execution {}", execution, propagatingExecution);
      propagatingExecution.initialize();
    } else {
      propagatingExecution = execution;
    }
    propagatingExecution.performOperation(AtomicOperation.TRANSITION_NOTIFY_LISTENER_START);
    log.debug("{} perform {}: {}", execution, this, activity);
  }

  @Override
  public String toString() {
    return "transition_create_scope";
  }
}
