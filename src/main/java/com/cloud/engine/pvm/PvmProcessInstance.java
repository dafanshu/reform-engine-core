package com.cloud.engine.pvm;

import java.util.List;

public interface PvmProcessInstance extends PvmExecution {

  void start();

  PvmExecution findExecution(String activityId);

  List<String> findActiveActivityIds();

  boolean isEnded();

  void deleteCascade(String deleteReason);

}
