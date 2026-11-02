package com.cloud.engine.pvm.delegate;

public interface SignallableActivityBehavior extends ActivityBehavior {

  void signal(ActivityExecution execution, String signalEvent, Object signalData) throws Exception;

}
