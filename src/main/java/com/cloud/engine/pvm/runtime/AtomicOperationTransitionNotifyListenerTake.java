package com.cloud.engine.pvm.runtime;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import com.cloud.engine.pvm.process.TransitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AtomicOperationTransitionNotifyListenerTake implements AtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationTransitionNotifyListenerTake.class);

  @Override
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }

  @Override
  public void execute(InterpretableExecution execution) {
    TransitionImpl transition = execution.getTransition();
    List<ExecutionListener> executionListeners = transition.getExecutionListeners();
    int executionListenerIndex = execution.getExecutionListenerIndex();

    if (executionListeners.size()>executionListenerIndex) {
      execution.setEventName(com.cloud.engine.pvm.PvmEvent.EVENTNAME_TAKE);
      execution.setEventSource(transition);
      ExecutionListener listener = executionListeners.get(executionListenerIndex);
      try {
        listener.notify(execution);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new PvmException("couldn't execute event listener : "+e.getMessage(), e);
      }
      execution.setExecutionListenerIndex(executionListenerIndex+1);
      execution.performOperation(this);
      ActivityImpl activity = (ActivityImpl) execution.getActivity();
      log.debug("{} performOperation {}: {}", execution, this, activity);
    } else {
        if (log.isDebugEnabled()) {
                log.debug("{} takes transition {}", execution, transition);
        }
      execution.setExecutionListenerIndex(0);
      execution.setEventName(null);
      execution.setEventSource(null);

      ActivityImpl activity = (ActivityImpl) execution.getActivity();
      ActivityImpl nextScope = findNextScope(activity.getParent(), transition.getDestination());
      execution.setActivity(nextScope);

      execution.performOperation(TRANSITION_CREATE_SCOPE);
      log.debug("{} perform {}: {}", execution, this, activity);
    }
  }

  /** finds the next scope to enter.  the most outer scope is found first */
  public static ActivityImpl findNextScope(ScopeImpl outerScopeElement, ActivityImpl destination) {
    ActivityImpl nextScope = destination;
    while( (nextScope.getParent() instanceof ActivityImpl)
           && (nextScope.getParent() != outerScopeElement)
         ) {
      nextScope = (ActivityImpl) nextScope.getParent();
    }
    return nextScope;
  }

  @Override
  public String toString() {
    return "transition_notify_listener_take";
  }
}
