package pvm.ucase.behavior;

import com.cloud.engine.pvm.PvmActivity;
import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParallelGateway implements ActivityBehavior {

  private static final long serialVersionUID = 1L;

  private static Logger log = LoggerFactory.getLogger(ParallelGateway.class);

  @Override
  public void execute(ActivityExecution execution) {
    PvmActivity activity = execution.getActivity();

    List<PvmTransition> outgoingTransitions = execution.getActivity().getOutgoingTransitions();

    execution.inactivate();

    List<ActivityExecution> joinedExecutions = execution.findInactiveConcurrentExecutions(activity);

    int nbrOfExecutionsToJoin = execution.getActivity().getIncomingTransitions().size();
    int nbrOfExecutionsJoined = joinedExecutions.size();

    if (nbrOfExecutionsJoined==nbrOfExecutionsToJoin) {
      log.debug("parallel gateway '{}' activates: {} of {} joined", activity.getId(), nbrOfExecutionsJoined, nbrOfExecutionsToJoin);
      execution.takeAll(outgoingTransitions, joinedExecutions);

    } else if (log.isDebugEnabled()){
      log.debug("parallel gateway '{}' does not activate: {} of {} joined", activity.getId(), nbrOfExecutionsJoined, nbrOfExecutionsToJoin);
    }
  }
}
