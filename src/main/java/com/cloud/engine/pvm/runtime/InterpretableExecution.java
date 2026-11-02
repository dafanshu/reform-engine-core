package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.PvmProcessElement;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.ExecutionListenerExecution;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.process.TransitionImpl;

public interface InterpretableExecution extends ActivityExecution, ExecutionListenerExecution, PvmProcessInstance {

  @Override
  void take(PvmTransition transition);

  void take(PvmTransition transition, boolean fireActivityCompletedEvent);

  void setEventName(String eventName);

  void setEventSource(PvmProcessElement element);

  Integer getExecutionListenerIndex();

  void setExecutionListenerIndex(Integer executionListenerIndex);

  ProcessDefinitionImpl getProcessDefinition();

  void setActivity(ActivityImpl activity);

  void performOperation(AtomicOperation etomicOperation);

  @Override
  boolean isScope();

  void destroy();

  void remove();

  InterpretableExecution getReplacedBy();
  void setReplacedBy(InterpretableExecution replacedBy);

  InterpretableExecution getSubProcessInstance();
  void setSubProcessInstance(InterpretableExecution subProcessInstance);

  InterpretableExecution getSuperExecution();

  @Override
  void deleteCascade(String deleteReason);

  boolean isDeleteRoot();

  TransitionImpl getTransition();
  void setTransition(TransitionImpl object);

  void initialize();

  void setParent(InterpretableExecution parent);

  void setProcessDefinition(ProcessDefinitionImpl processDefinitionImpl);

  void setProcessInstance(InterpretableExecution processInstance);

  boolean isEventScope();

  void setEventScope(boolean isEventScope);

  StartingExecution getStartingExecution();

  void disposeStartingExecution();

  boolean shouldExecuteListeners();
}
