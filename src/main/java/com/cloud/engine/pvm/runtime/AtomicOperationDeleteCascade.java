package com.cloud.engine.pvm.runtime;

import java.util.List;

public class AtomicOperationDeleteCascade implements AtomicOperation {

  @Override
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }

  @Override
  public void execute(InterpretableExecution execution) {
    InterpretableExecution firstLeaf = findFirstLeaf(execution);

    if (firstLeaf.getSubProcessInstance()!=null) {
      firstLeaf.getSubProcessInstance().deleteCascade(execution.getDeleteReason());
    }

    firstLeaf.performOperation(AtomicOperation.DELETE_CASCADE_FIRE_ACTIVITY_END);
  }

  @SuppressWarnings("unchecked")
  protected InterpretableExecution findFirstLeaf(InterpretableExecution execution) {
    List<InterpretableExecution> executions = (List<InterpretableExecution>) execution.getExecutions();
    if (!executions.isEmpty()) {
      return findFirstLeaf(executions.get(0));
    }
    return execution;
  }
}
