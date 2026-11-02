package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AtomicOperationProcessStart extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationProcessStart.class);

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return execution.getProcessDefinition();
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_START;
  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    ProcessDefinitionImpl processDefinition = execution.getProcessDefinition();
    StartingExecution startingExecution = execution.getStartingExecution();
    List<ActivityImpl> initialActivityStack = processDefinition.getInitialActivityStack(startingExecution.getInitial());
    ActivityImpl activity = initialActivityStack.get(0);
    // Scoped activities run on the child already created during instance initialization.
    InterpretableExecution executionToUse = activity.isScope()
        ? (InterpretableExecution) execution.getExecutions().get(0) : execution;
    executionToUse.setActivity(activity);
    executionToUse.performOperation(PROCESS_START_INITIAL);
    log.debug("{} perform {}: {}", execution, this, activity);
  }

  @Override
  public String toString() {
    return "process_start";
  }
}
