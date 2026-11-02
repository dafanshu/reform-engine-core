package com.cloud.engine.pvm;

import com.cloud.engine.ActivitiException;
import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.delegate.ActivityBehavior;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.process.ProcessElementImpl;
import com.cloud.engine.pvm.process.ScopeImpl;
import com.cloud.engine.pvm.process.TransitionImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author yurui
 * @Description: 流程构造器
 * @Date: 16:35 2019/8/7
 * Copyright (c) huidiancloud
*/
public class ProcessDefinitionBuilder {

  protected ProcessDefinitionImpl processDefinition;
  protected Stack<ScopeImpl> scopeStack = new Stack<ScopeImpl>();
  protected ProcessElementImpl processElement = processDefinition;
  protected TransitionImpl transition;
  protected List<Object[]> unresolvedTransitions = new ArrayList<Object[]>();

  public ProcessDefinitionBuilder() {
    this(null);
  }

  public ProcessDefinitionBuilder(String processDefinitionId) {
    processDefinition = new ProcessDefinitionImpl(processDefinitionId);
    scopeStack.push(processDefinition);
  }

  public ProcessDefinitionBuilder createActivity(String id) {
    ActivityImpl activity = scopeStack.peek().createActivity(id);
    scopeStack.push(activity);
    processElement = activity;

    transition = null;

    return this;
  }

  public ProcessDefinitionBuilder endActivity() {
    scopeStack.pop();
    processElement = scopeStack.peek();

    transition = null;

    return this;
  }

  public ProcessDefinitionBuilder initial() {
    processDefinition.setInitial(getActivity());
    return this;
  }

  public ProcessDefinitionBuilder startTransition(String destinationActivityId) {
    return startTransition(destinationActivityId, null);
  }

  public ProcessDefinitionBuilder startTransition(String destinationActivityId, String transitionId) {
    if (destinationActivityId==null) {
      throw new PvmException("destinationActivityId is null");
    }
    ActivityImpl activity = getActivity();
    transition = activity.createOutgoingTransition(transitionId);
    unresolvedTransitions.add(new Object[]{transition, destinationActivityId});
    processElement = transition;
    return this;
  }

  public ProcessDefinitionBuilder endTransition() {
    processElement = scopeStack.peek();
    transition = null;
    return this;
  }

  public ProcessDefinitionBuilder transition(String destinationActivityId) {
    return transition(destinationActivityId, null);
  }

  public ProcessDefinitionBuilder transition(String destinationActivityId, String transitionId) {
    startTransition(destinationActivityId, transitionId);
    endTransition();
    return this;
  }

  public ProcessDefinitionBuilder behavior(ActivityBehavior activityBehaviour) {
    getActivity().setActivityBehavior(activityBehaviour);
    return this;
  }

  public ProcessDefinitionBuilder property(String name, Object value) {
    processElement.setProperty(name, value);
    return this;
  }

  public PvmProcessDefinition buildProcessDefinition() {
    for (Object[] unresolvedTransition: unresolvedTransitions) {
      TransitionImpl transition = (TransitionImpl) unresolvedTransition[0];
      String destinationActivityName = (String) unresolvedTransition[1];
      ActivityImpl destination = processDefinition.findActivity(destinationActivityName);
      if (destination == null) {
        throw new ActivitiException("destination '"+destinationActivityName+"' not found.  (referenced from transition in '"+transition.getSource().getId()+"')");
      }
      transition.setDestination(destination);
    }
    return processDefinition;
  }

  protected ActivityImpl getActivity() {
    return (ActivityImpl) scopeStack.peek();
  }

  public ProcessDefinitionBuilder scope() {
    getActivity().setScope(true);
    return this;
  }

  public ProcessDefinitionBuilder executionListener(ExecutionListener executionListener) {
    if (transition!=null) {
      transition.addExecutionListener(executionListener);
    } else {
      throw new PvmException("not in a transition scope");
    }
    return this;
  }

  public ProcessDefinitionBuilder executionListener(String eventName, ExecutionListener executionListener) {
    if (transition==null) {
      scopeStack.peek().addExecutionListener(eventName, executionListener);
    } else {
      throw new PvmException("not in an activity- or process definition scope. (but in a transition scope)");
    }
    return this;
  }
  //*******************设置基础信息**************
  public ProcessDefinitionBuilder setKey(String key){
    this.processDefinition.setKey(key);
    return this;
  }

  public ProcessDefinitionBuilder setName(String name){
    this.processDefinition.setName(name);
    return this;
  }

  public ProcessDefinitionBuilder setDescription(String description){
    this.processDefinition.setDescription(description);
    return this;
  }
}
