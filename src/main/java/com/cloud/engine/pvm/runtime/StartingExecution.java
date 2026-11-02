package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.process.ActivityImpl;

public class StartingExecution {

  protected final ActivityImpl selectedInitial;

  public StartingExecution(ActivityImpl selectedInitial) {
    this.selectedInitial = selectedInitial;
  }

  public ActivityImpl getInitial() {
    return selectedInitial;
  }

}
