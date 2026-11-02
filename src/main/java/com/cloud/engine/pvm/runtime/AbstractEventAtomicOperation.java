package com.cloud.engine.pvm.runtime;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.process.ScopeImpl;

import java.util.List;

public abstract class AbstractEventAtomicOperation implements AtomicOperation {

  @Override
  public boolean isAsync(InterpretableExecution execution) {
    return false;
  }

  @Override
  public void execute(InterpretableExecution execution) {
    ScopeImpl scope = getScope(execution);
    List<ExecutionListener> exectionListeners = scope.getExecutionListeners(getEventName());
    int executionListenerIndex = execution.getExecutionListenerIndex();

    if (exectionListeners.size() > executionListenerIndex) {
      execution.setEventName(getEventName());
      execution.setEventSource(scope);
      if (execution.shouldExecuteListeners()) {
        ExecutionListener listener = exectionListeners.get(executionListenerIndex);
        try {
          listener.notify(execution);
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new PvmException("couldn't execute event listener : " + e.getMessage(), e);
        }
      }
      execution.setExecutionListenerIndex(executionListenerIndex+1);
      execution.performOperation(this);

    } else {
      execution.setExecutionListenerIndex(0);
      execution.setEventName(null);
      execution.setEventSource(null);

      eventNotificationsCompleted(execution);
    }
  }

  protected abstract ScopeImpl getScope(InterpretableExecution execution);
  protected abstract String getEventName();
  protected abstract void eventNotificationsCompleted(InterpretableExecution execution);
}
