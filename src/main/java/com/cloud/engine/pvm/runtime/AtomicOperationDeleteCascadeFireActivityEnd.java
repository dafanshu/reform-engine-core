package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationDeleteCascadeFireActivityEnd extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationDeleteCascadeFireActivityEnd.class);

  @Override
  public void execute(InterpretableExecution execution) {
    // Scope cleanup may resolve its event source through inactive parents all
    // the way to the process definition. Only the process root owns that event;
    // a cancelled scope must still be removed without notifying process end.
    if (!execution.isProcessInstanceType() && getScope(execution) == execution.getProcessDefinition()) {
      eventNotificationsCompleted(execution);
    } else {
      super.execute(execution);
    }
  }

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();

    if (activity!=null) {
      return activity;
    } else {
      InterpretableExecution parent = (InterpretableExecution) execution.getParent();
      if (parent != null) {
        return getScope((InterpretableExecution) execution.getParent());
      }
      return execution.getProcessDefinition();
    }
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    log.debug("{} executes {}: {}", execution, activity);
    if ( (execution.isScope())
            && (activity!=null)
          )  {
      execution.setActivity(activity.getParentActivity());
      execution.performOperation(AtomicOperation.DELETE_CASCADE_FIRE_ACTIVITY_END);

    } else {
      if (execution.isScope()) {
        execution.destroy();
      }

      execution.remove();

      if (!execution.isDeleteRoot()) {
        InterpretableExecution parent = (InterpretableExecution) execution.getParent();
        if (parent!=null) {
          parent.performOperation(AtomicOperation.DELETE_CASCADE);
        }
      }
    }
  }
}
