package com.cloud.engine.pvm.process;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.delegate.Expression;
import com.cloud.engine.pvm.PvmTransition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransitionImpl extends ProcessElementImpl implements PvmTransition {

  private static final long serialVersionUID = 1L;

  protected ActivityImpl source;
  protected ActivityImpl destination;
  protected List<ExecutionListener> executionListeners;
  protected Expression skipExpression;

  public TransitionImpl(String id, Expression skipExpression, ProcessDefinitionImpl processDefinition) {
    super(id, processDefinition);
    this.skipExpression = skipExpression;
  }

  @Override
  public ActivityImpl getSource() {
    return source;
  }

  public void setDestination(ActivityImpl destination) {
    this.destination = destination;
    destination.getIncomingTransitions().add(this);
  }

  public void addExecutionListener(ExecutionListener executionListener) {
    if (executionListeners==null) {
      executionListeners = new ArrayList<ExecutionListener>();
    }
    executionListeners.add(executionListener);
  }

  @Override
  public String toString() {
    return "("+source.getId()+")--"+(id!=null?id+"-->(":">(")+destination.getId()+")";
  }

  public List<ExecutionListener> getExecutionListeners() {
    if (executionListeners==null) {
      return Collections.EMPTY_LIST;
    }
    return executionListeners;
  }

  protected void setSource(ActivityImpl source) {
    this.source = source;
  }

  @Override
  public ActivityImpl getDestination() {
    return destination;
  }

  public void setExecutionListeners(List<ExecutionListener> executionListeners) {
    this.executionListeners = executionListeners;
  }

  @Override
  public Expression getSkipExpression() {
    return skipExpression;
  }

  public void setSkipExpression(Expression skipExpression) {
    this.skipExpression = skipExpression;
  }
}
