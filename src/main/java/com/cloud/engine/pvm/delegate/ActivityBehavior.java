package com.cloud.engine.pvm.delegate;

import java.io.Serializable;

public interface ActivityBehavior extends Serializable {

  void execute(ActivityExecution execution) throws Exception;

}
