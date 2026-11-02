package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.PvmTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutgoingExecution {

  private static Logger log = LoggerFactory.getLogger(OutgoingExecution.class);

  protected InterpretableExecution outgoingExecution;
  protected PvmTransition outgoingTransition;
  protected boolean isNew;

  public OutgoingExecution(InterpretableExecution outgoingExecution, PvmTransition outgoingTransition, boolean isNew) {
    this.outgoingExecution = outgoingExecution;
    this.outgoingTransition = outgoingTransition;
    this.isNew = isNew;
  }

  public void take() {
        take(true);
  }

  public void take(boolean fireActivityCompletedEvent) {
    if (outgoingExecution.getReplacedBy()!=null) {
      outgoingExecution = outgoingExecution.getReplacedBy();
    }
    if(!outgoingExecution.isDeleteRoot()) {
      outgoingExecution.take(outgoingTransition, fireActivityCompletedEvent);
    } else {
      log.debug("Not taking transition '{}', outgoing execution has ended.", outgoingTransition);
    }
  }
}