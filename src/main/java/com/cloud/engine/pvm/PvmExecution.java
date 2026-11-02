package com.cloud.engine.pvm;

import java.util.Map;

public interface PvmExecution {

  void signal(String signalName, Object signalData);

  PvmActivity getActivity();

  boolean hasVariable(String variableName);

  void setVariable(String variableName, Object value);

  Object getVariable(String variableName);

  Map<String, Object> getVariables();

}
