package com.cloud.engine.pvm.delegate;

public interface CompositeActivityBehavior extends ActivityBehavior {

  void lastExecutionEnded(ActivityExecution execution);
}
