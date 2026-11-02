package com.cloud.engine.process.impl;

import com.cloud.engine.process.IBehaviorDetecter;
import com.cloud.engine.pvm.delegate.ActivityBehavior;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yurui
 * @Description: 用于查询流程元素对应的抽象行为
 * @Date: 11:49 2019/8/7
 * Copyright (c) huidiancloud
*/
public class SimpleBehaviorDetecterImpl implements IBehaviorDetecter{

    private Map<String, ActivityBehavior> behaviors = new HashMap<>();

    @Override
    public void put(String shape, ActivityBehavior activityBehavior) {
        this.behaviors.put(shape, activityBehavior);
    }

    @Override
    public ActivityBehavior getBehavior(String shape) {
        return this.behaviors.get(shape);
    }

    @Override
    public void setBehavior(Map<String, ActivityBehavior> behaviors) {
        this.behaviors = behaviors;
    }
}
