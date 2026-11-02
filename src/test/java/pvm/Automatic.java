package pvm;

import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.delegate.ActivityExecution;

import java.util.List;

public class Automatic implements ActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    List<PvmTransition> outgoingTransitions = execution.getActivity().getOutgoingTransitions();
    if(outgoingTransitions.isEmpty()) {
      execution.end();
    } else {
      execution.take(outgoingTransitions.get(0));
    }
  }
}
