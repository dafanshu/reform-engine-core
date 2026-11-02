package com.cloud.engine.pvm.runtime;

import com.cloud.engine.ActivitiException;
import com.cloud.engine.pvm.delegate.SubProcessActivityBehavior;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationProcessEnd extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationProcessEnd.class);

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return execution.getProcessDefinition();
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    InterpretableExecution superExecution = execution.getSuperExecution();
    SubProcessActivityBehavior subProcessActivityBehavior = null;
    log.debug("{} executes {}: {}", execution, null);
    // copy variables before destroying the ended sub process instance
    if (superExecution!=null) {
      ActivityImpl activity = (ActivityImpl) superExecution.getActivity();
      subProcessActivityBehavior = (SubProcessActivityBehavior) activity.getActivityBehavior();
      try {
        subProcessActivityBehavior.completing(superExecution, execution);
      } catch (RuntimeException e) {
          log.error("Error while completing sub process of execution {}", execution, e);
          throw e;
      } catch (Exception e) {
          log.error("Error while completing sub process of execution {}", execution, e);
          throw new ActivitiException("Error while completing sub process of execution " + execution, e);
      }
    }
    execution.destroy();
    execution.remove();
    // and trigger execution afterwards
    if (superExecution!=null) {
      superExecution.setSubProcessInstance(null);
      try {
          subProcessActivityBehavior.completed(superExecution);
      } catch (RuntimeException e) {
          log.error("Error while completing sub process of execution {}", execution, e);
          throw e;
      } catch (Exception e) {
          log.error("Error while completing sub process of execution {}", execution, e);
          throw new ActivitiException("Error while completing sub process of execution " + execution, e);
      }
    }
  }

    @Override
    public String toString() {
        return "process_end";
    }
}
