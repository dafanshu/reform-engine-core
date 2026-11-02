package com.cloud.engine.pvm;

public interface PvmProcessDefinition extends ReadOnlyProcessDefinition {

  PvmProcessInstance createProcessInstance();

}
