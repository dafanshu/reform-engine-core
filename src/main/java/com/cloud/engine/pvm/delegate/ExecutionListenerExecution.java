package com.cloud.engine.pvm.delegate;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.PvmProcessElement;

public interface ExecutionListenerExecution extends DelegateExecution {

  @Override
  String getEventName();

  PvmProcessElement getEventSource();

  String getDeleteReason();
}
