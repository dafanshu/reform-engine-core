package com.cloud.engine.pvm.delegate;

import com.cloud.engine.delegate.DelegateExecution;

public interface SubProcessActivityBehavior extends ActivityBehavior {

  /** called before the process instance is destroyed to allow
   * this activity to extract data from the sub process instance.
   * No control flow should be done on the execution yet. */
  void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception;

  /** called after the process instance is destroyed for this activity to perform its outgoing control flow logic. */
  void completed(ActivityExecution execution) throws Exception;
}
