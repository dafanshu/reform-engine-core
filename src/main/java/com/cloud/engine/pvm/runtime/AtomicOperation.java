package com.cloud.engine.pvm.runtime;

public interface AtomicOperation {

  AtomicOperation PROCESS_START = new AtomicOperationProcessStart();
  AtomicOperation PROCESS_START_INITIAL = new AtomicOperationProcessStartInitial();
  AtomicOperation PROCESS_END = new AtomicOperationProcessEnd();
  AtomicOperation ACTIVITY_START = new AtomicOperationActivityStart();
  AtomicOperation ACTIVITY_EXECUTE = new AtomicOperationActivityExecute();
  AtomicOperation ACTIVITY_END = new AtomicOperationActivityEnd();
  AtomicOperation TRANSITION_NOTIFY_LISTENER_END = new AtomicOperationTransitionNotifyListenerEnd();
  AtomicOperation TRANSITION_DESTROY_SCOPE = new AtomicOperationTransitionDestroyScope();
  AtomicOperation TRANSITION_NOTIFY_LISTENER_TAKE = new AtomicOperationTransitionNotifyListenerTake();
  AtomicOperation TRANSITION_CREATE_SCOPE = new AtomicOperationTransitionCreateScope();
  AtomicOperation TRANSITION_NOTIFY_LISTENER_START = new AtomicOperationTransitionNotifyListenerStart();

  AtomicOperation DELETE_CASCADE = new AtomicOperationDeleteCascade();
  AtomicOperation DELETE_CASCADE_FIRE_ACTIVITY_END = new AtomicOperationDeleteCascadeFireActivityEnd();

  void execute(InterpretableExecution execution);

  boolean isAsync(InterpretableExecution execution);
}
