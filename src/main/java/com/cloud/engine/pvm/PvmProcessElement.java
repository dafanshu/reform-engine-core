package com.cloud.engine.pvm;

import java.io.Serializable;

public interface PvmProcessElement extends Serializable {

  String getId();

  PvmProcessDefinition getProcessDefinition();

  Object getProperty(String name);

}
