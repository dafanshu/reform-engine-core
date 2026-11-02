package com.cloud.engine.pvm.runtime;

import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.CompositeActivityBehavior;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AtomicOperationActivityEnd extends AbstractEventAtomicOperation {

  private static Logger log = LoggerFactory.getLogger(AtomicOperationActivityEnd.class);

  @Override
  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivity();
  }

  @Override
  protected String getEventName() {
    return com.cloud.engine.pvm.PvmEvent.EVENTNAME_END;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    ActivityImpl parentActivity = activity.getParentActivity();
    log.debug("{} performOperation {}: {}", execution, this, activity);
    // if the execution is a single path of execution inside the process definition scope
    if ( (parentActivity!=null) &&(!parentActivity.isScope()) ) {
      execution.setActivity(parentActivity);
      execution.performOperation(ACTIVITY_END);
    } else if (execution.isProcessInstanceType()) {
      execution.performOperation(PROCESS_END);
    } else if (execution.isScope()) {
      ActivityBehavior parentActivityBehavior = (parentActivity!=null ? parentActivity.getActivityBehavior() : null);
      if (parentActivityBehavior instanceof CompositeActivityBehavior) {
        CompositeActivityBehavior compositeActivityBehavior = (CompositeActivityBehavior) parentActivity.getActivityBehavior();

        if(activity.isScope() && activity.getOutgoingTransitions().isEmpty()) {
          // there is no transition destroying the scope
          InterpretableExecution parentScopeExecution = (InterpretableExecution) execution.getParent();
          execution.destroy();
          execution.remove();
          parentScopeExecution.setActivity(parentActivity);
          compositeActivityBehavior.lastExecutionEnded(parentScopeExecution);
        } else {
          execution.setActivity(parentActivity);
          compositeActivityBehavior.lastExecutionEnded(execution);
        }
      } else {
        // default destroy scope behavior
        InterpretableExecution parentScopeExecution = (InterpretableExecution) execution.getParent();
        execution.destroy();
        execution.remove();
        // if we are a scope under the process instance
        // and have no outgoing transitions: end the process instance here
        if(activity.getParent() == activity.getProcessDefinition()) {
          parentScopeExecution.setActivity(activity);
          if(activity.getOutgoingTransitions().isEmpty()) {
              // we call end() because it sets isEnded on the execution
              parentScopeExecution.end();
          } else {
                  parentScopeExecution.performOperation(PROCESS_END);
          }
        } else {
          parentScopeExecution.setActivity(parentActivity);
          parentScopeExecution.performOperation(ACTIVITY_END);
        }
      }
    } else {
      // execution.isConcurrent() && !execution.isScope()
      execution.remove();
      // prune if necessary
      InterpretableExecution concurrentRoot = (InterpretableExecution) execution.getParent();
      if (concurrentRoot.getExecutions().size()==1) {
        InterpretableExecution lastConcurrent = (InterpretableExecution) concurrentRoot.getExecutions().get(0);
        if (!lastConcurrent.isScope()) {
          concurrentRoot.setActivity((ActivityImpl) lastConcurrent.getActivity());
          lastConcurrent.setReplacedBy(concurrentRoot);
          // Move children of lastConcurrent one level up
          if (!lastConcurrent.getExecutions().isEmpty()) {
            concurrentRoot.getExecutions().clear();
            for (ActivityExecution childExecution : lastConcurrent.getExecutions()) {
              InterpretableExecution childInterpretableExecution = (InterpretableExecution) childExecution;
              ((List)concurrentRoot.getExecutions()).add(childExecution); // casting ... damn generics
              childInterpretableExecution.setParent(concurrentRoot);
            }
            lastConcurrent.getExecutions().clear();
          }
          // Copy execution-local variables of lastConcurrent
          concurrentRoot.setVariablesLocal(lastConcurrent.getVariablesLocal());
          // Make sure parent execution is re-activated when the last concurrent child execution is active
          if (!concurrentRoot.isActive() && lastConcurrent.isActive()) {
            concurrentRoot.setActive(true);
          }
          lastConcurrent.remove();
        } else {
          lastConcurrent.setConcurrent(false);
        }
      }
    }
  }

  protected boolean isExecutionAloneInParent(InterpretableExecution execution) {
    ScopeImpl parentScope = (ScopeImpl) execution.getActivity().getParent();
    for (InterpretableExecution other: (List<InterpretableExecution>) execution.getParent().getExecutions()) {
      if (other!=execution && parentScope.contains((ActivityImpl) other.getActivity())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "activity_end";
  }
}
