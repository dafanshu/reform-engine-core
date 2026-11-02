package com.cloud.engine.pvm.process;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.PvmScope;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ScopeImpl extends ProcessElementImpl implements PvmScope {

  private static final long serialVersionUID = 1L;

  protected List<ActivityImpl> activities = new ArrayList<ActivityImpl>();
  protected Map<String, ActivityImpl> namedActivities = new HashMap<>();
  protected Map<String, List<ExecutionListener>> executionListeners = new HashMap<String, List<ExecutionListener>>();

  public ScopeImpl(String id, ProcessDefinitionImpl processDefinition) {
    super(id, processDefinition);
  }

  @Override
  public ActivityImpl findActivity(String activityId) {
    ActivityImpl localActivity = namedActivities.get(activityId);
    if (localActivity!=null) {
      return localActivity;
    }
    for (ActivityImpl activity: activities) {
      ActivityImpl nestedActivity = activity.findActivity(activityId);
      if (nestedActivity!=null) {
        return nestedActivity;
      }
    }
    return null;
  }

  public ActivityImpl createActivity() {
    return createActivity(null);
  }

  public ActivityImpl createActivity(String activityId) {
      return createActivity(activityId, null);
  }

  public ActivityImpl createActivity(String activityId, String activityName) {
    ActivityImpl activity = new ActivityImpl(activityId, processDefinition);
    if (StringUtils.isNotEmpty(activityName)){
        activity.setProperty("name", activityName);
    }
    if (activityId!=null) {
      if (processDefinition.findActivity(activityId) != null) {
        throw new PvmException("duplicate activity id '" + activityId + "'");
      }
      namedActivities.put(activityId, activity);
    }
    activity.setParent(this);
    activities.add(activity);
    return  activity;
  }

  public boolean contains(ActivityImpl activity) {
    if (namedActivities.containsKey(activity.getId())) {
      return true;
    }
    for (ActivityImpl nestedActivity : activities) {
      if (nestedActivity.contains(activity)) {
        return true;
      }
    }
    return false;
  }

  public List<ExecutionListener> getExecutionListeners(String eventName) {
    List<ExecutionListener> executionListenerList = getExecutionListeners().get(eventName);
    if (executionListenerList!=null) {
      return executionListenerList;
    }
    return Collections.EMPTY_LIST;
  }

  public void addExecutionListener(String eventName, ExecutionListener executionListener) {
    addExecutionListener(eventName, executionListener, -1);
  }

  public void addExecutionListener(String eventName, ExecutionListener executionListener, int index) {
    List<ExecutionListener> listeners = executionListeners.get(eventName);
    if (listeners==null) {
      listeners = new ArrayList<ExecutionListener>();
      executionListeners.put(eventName, listeners);
    }
    if (index<0) {
      listeners.add(executionListener);
    } else {
      listeners.add(index, executionListener);
    }
  }

  public Map<String, List<ExecutionListener>> getExecutionListeners() {
    return executionListeners;
  }

  @Override
  public List<ActivityImpl> getActivities() {
    return activities;
  }

}
