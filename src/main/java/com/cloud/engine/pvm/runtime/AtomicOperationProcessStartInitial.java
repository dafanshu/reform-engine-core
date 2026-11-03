package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AtomicOperationProcessStartInitial extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationProcessStartInitial.class);

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
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    ProcessDefinitionImpl processDefinition = execution.getProcessDefinition();
    // The selected initial activity belongs to the process instance, not each scope child.
    InterpretableExecution processInstance = execution.isProcessInstanceType()
        ? execution : (InterpretableExecution) execution.getProcessInstance();
    StartingExecution startingExecution = processInstance.getStartingExecution();
    if (activity==startingExecution.getInitial()) {
      processInstance.disposeStartingExecution();
      execution.performOperation(ACTIVITY_EXECUTE);
    } else {
      List<ActivityImpl> initialActivityStack = processDefinition.getInitialActivityStack(startingExecution.getInitial());
      int index = initialActivityStack.indexOf(activity);
      activity = initialActivityStack.get(index+1);

      InterpretableExecution executionToUse = null;
      if (activity.isScope()) {
        executionToUse = (InterpretableExecution) execution.getExecutions().get(0);
        execution.setActivity(null);
      } else {
        executionToUse = execution;
      }
      executionToUse.setActivity(activity);
      executionToUse.performOperation(PROCESS_START_INITIAL);
      log.debug("{} perform {}: {}", execution, this, activity);
    }
  }

  @Override
  public String toString() {
    return "process_start_initial";
  }
}
