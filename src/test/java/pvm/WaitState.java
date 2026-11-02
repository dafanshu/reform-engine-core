package pvm;

import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.SignallableActivityBehavior;

import java.util.Objects;

public class WaitState implements SignallableActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) {
    // By default, the execution will not propagate.
    // So if no method like take(Transition) is called on execution
    // then the activity will behave as a wait state.  The execution is currently
    // pointing to the activity.  The original call to execution.start()
    // or execution.event() will return.  Then the execution object will
    // remain pointing to the current activity until execution.event(Object) is called.
    // That method will delegate to the method below.
  }

  public void signal(ActivityExecution execution, String signalName, Object event) {
    PvmTransition transition = findTransition(execution, signalName);
    if (Objects.isNull(transition)){
      execution.end();
    }else{
      execution.take(transition);
    }
  }

  protected PvmTransition findTransition(ActivityExecution execution, String signalName) {
    for (PvmTransition transition: execution.getActivity().getOutgoingTransitions()) {
      if (signalName==null) {
        if (transition.getId()==null) {
          return transition;
        }
      } else {
        if (signalName.equals(transition.getId())) {
          return transition;
        }
      }
    }
    //throw new RuntimeException("no transition for signalName '"+signalName+"' in WaitState '"+execution.getActivity().getId()+"'");
    return null;
  }
}
