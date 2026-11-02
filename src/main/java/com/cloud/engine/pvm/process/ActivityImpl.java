package com.cloud.engine.pvm.process;

import com.cloud.engine.delegate.Expression;
import com.cloud.engine.pvm.PvmActivity;
import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.PvmTransition;
import com.cloud.engine.pvm.delegate.ActivityBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yurui
 * @Description: 环节抽象类
 * @Date: 18:14 2019/8/23
 * Copyright (c) huidiancloud
 */
public class ActivityImpl extends ScopeImpl implements PvmActivity {
    private static final long serialVersionUID = 1L;
    /**
     * 输出线路
     **/
    protected List<TransitionImpl> outgoingTransitions = new ArrayList<>();
    /**
     * 输入线路
     **/
    protected List<TransitionImpl> incomingTransitions = new ArrayList<TransitionImpl>();
    /**
     * 命名输出线路
     **/
    protected Map<String, TransitionImpl> namedOutgoingTransitions = new HashMap<>();
    /**
     * 环节参数
     **/
    protected Map<String, Object> variables;
    /**
     * 环节行为
     **/
    protected ActivityBehavior activityBehavior;
    protected ScopeImpl parent;
    protected boolean isScope;
    protected boolean isAsync;
    /**
     * 暂时不用关心
     **/
    protected boolean isExclusive;

    public ActivityImpl(String id, ProcessDefinitionImpl processDefinition) {
        super(id, processDefinition);
    }

    public TransitionImpl createOutgoingTransition() {
        return createOutgoingTransition(null);
    }

    public TransitionImpl createOutgoingTransition(String transitionId) {
        return createOutgoingTransition(transitionId, null);
    }

    public TransitionImpl createOutgoingTransition(String transitionId, Expression skipExpression) {
        TransitionImpl transition = new TransitionImpl(transitionId, skipExpression, processDefinition);
        transition.setSource(this);
        outgoingTransitions.add(transition);
        if (transitionId != null) {
            if (namedOutgoingTransitions.containsKey(transitionId)) {
                throw new PvmException("activity '" + id + " has duplicate transition '" + transitionId + "'");
            }
            namedOutgoingTransitions.put(transitionId, transition);
        }
        return transition;
    }

    @Override
    public TransitionImpl findOutgoingTransition(String transitionId) {
        return namedOutgoingTransitions.get(transitionId);
    }

    @Override
    public String toString() {
        return "Activity(" + id + ")";
    }

    public ActivityImpl getParentActivity() {
        if (parent instanceof ActivityImpl) {
            return (ActivityImpl) parent;
        }
        return null;
    }

    protected void setOutgoingTransitions(List<TransitionImpl> outgoingTransitions) {
        this.outgoingTransitions = outgoingTransitions;
    }

    protected void setParent(ScopeImpl parent) {
        this.parent = parent;
    }

    protected void setIncomingTransitions(List<TransitionImpl> incomingTransitions) {
        this.incomingTransitions = incomingTransitions;
    }

    @Override
    public List<PvmTransition> getOutgoingTransitions() {
        return (List) outgoingTransitions;
    }

    public ActivityBehavior getActivityBehavior() {
        return activityBehavior;
    }

    public void setActivityBehavior(ActivityBehavior activityBehavior) {
        this.activityBehavior = activityBehavior;
    }

    @Override
    public ScopeImpl getParent() {
        return parent;
    }

    @Override
    public List<PvmTransition> getIncomingTransitions() {
        return (List) incomingTransitions;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public boolean isScope() {
        return isScope;
    }

    public void setScope(boolean isScope) {
        this.isScope = isScope;
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    @Override
    public boolean isExclusive() {
        return isExclusive;
    }

    public void setExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }
}
