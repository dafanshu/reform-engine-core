package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.process.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtomicOperationActivityExecute implements AtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationActivityExecute.class);

  @Override
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }

  @Override
  public void execute(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    ActivityBehavior activityBehavior = activity.getActivityBehavior();
    if (activityBehavior==null) {
      throw new PvmException("no behavior specified in "+activity);
    }
    log.debug("{} executes {}: {}", execution, activity, activityBehavior.getClass().getName());
    try {
      activityBehavior.execute(execution);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new PvmException("couldn't execute activity <"+activity.getProperty("type")+" id=\""+activity.getId()+"\" ...>: "+e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return "activity_execute";
  }
}
