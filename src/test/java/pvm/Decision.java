package pvm;

import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.delegate.ActivityExecution;

public class Decision implements ActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    PvmTransition transition;
    String creditRating = (String) execution.getVariable("creditRating");
    if (creditRating.equals("AAA+")) {
      transition = execution.getActivity().findOutgoingTransition("wow");
    } else if (creditRating.equals("Aaa-")) {
      transition = execution.getActivity().findOutgoingTransition("nice");
    } else {
      transition = execution.getActivity().findOutgoingTransition("default");
    }

    execution.take(transition);
  }
}
