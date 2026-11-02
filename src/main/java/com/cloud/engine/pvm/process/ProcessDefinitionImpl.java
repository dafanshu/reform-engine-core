package com.cloud.engine.pvm.process;

import com.cloud.engine.ActivitiException;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import com.cloud.engine.pvm.runtime.InterpretableExecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessDefinitionImpl extends ScopeImpl implements PvmProcessDefinition {

  private static final long serialVersionUID = 1L;

  protected String name;
  protected String key;
  protected String description;
  protected ActivityImpl initial;
  protected Map<ActivityImpl, List<ActivityImpl>> initialActivityStacks = new HashMap<ActivityImpl, List<ActivityImpl>>();
  protected ParticipantProcess participantProcess;

  public ProcessDefinitionImpl(String id) {
    super(id, null);
    processDefinition = this;
  }

  @Override
  public PvmProcessInstance createProcessInstance() {
    if(initial == null) {
      throw new ActivitiException("Process '"+name+"' has no default start activity (e.g. none start event), hence you cannot use 'startProcessInstanceBy...' but have to start it using one of the modeled start events (e.g. message start events).");
    }
    return createProcessInstanceForInitial(initial);
  }

  public PvmProcessInstance createProcessInstanceForInitial(ActivityImpl initial) {

    if(initial == null) {
      throw new ActivitiException("Cannot start process instance, initial activity where the process instance should start is null.");
    }
    InterpretableExecution processInstance = newProcessInstance(initial);
    processInstance.setProcessDefinition(this);
    processInstance.setProcessInstance(processInstance);
    processInstance.initialize();
    InterpretableExecution scopeInstance = processInstance;
    List<ActivityImpl> initialActivityStack = getInitialActivityStack(initial);
    for (ActivityImpl initialActivity: initialActivityStack) {
      if (initialActivity.isScope()) {
        scopeInstance = (InterpretableExecution) scopeInstance.createExecution();
        scopeInstance.setActivity(initialActivity);
        if (initialActivity.isScope()) {
          scopeInstance.initialize();
        }
      }
    }
    scopeInstance.setActivity(initial);
    return processInstance;
  }

  public List<ActivityImpl> getInitialActivityStack() {
    return getInitialActivityStack(initial);
  }

  public synchronized List<ActivityImpl> getInitialActivityStack(ActivityImpl startActivity) {
    List<ActivityImpl> initialActivityStack = initialActivityStacks.get(startActivity);
    if(initialActivityStack == null) {
      initialActivityStack = new ArrayList<>();
      ActivityImpl activity = startActivity;
      while (activity!=null) {
        initialActivityStack.add(0, activity);
        activity = activity.getParentActivity();
      }
      initialActivityStacks.put(startActivity, initialActivityStack);
    }
    return initialActivityStack;
  }

  protected InterpretableExecution newProcessInstance(ActivityImpl startActivity) {
    return new ExecutionImpl(startActivity);
  }

  @Override
  public ActivityImpl getInitial() {
    return initial;
  }

  public void setInitial(ActivityImpl initial) {
    this.initial = initial;
  }

  @Override
  public String toString() {
    return "ProcessDefinition("+id+")";
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description){
    this.description = description;
  }

  public void setParticipantProcess(ParticipantProcess participantProcess) {
    this.participantProcess = participantProcess;
  }

  public ParticipantProcess getParticipantProcess() {
    return participantProcess;
  }
}
