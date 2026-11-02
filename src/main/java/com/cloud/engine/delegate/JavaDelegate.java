package com.cloud.engine.delegate;

public interface JavaDelegate {

  void execute(DelegateExecution execution) throws Exception;

}
