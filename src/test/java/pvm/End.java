package pvm;

import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.delegate.ActivityExecution;

public class End implements ActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    execution.end();
  }

}
