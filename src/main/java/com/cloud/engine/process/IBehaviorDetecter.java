package com.cloud.engine.process;

import com.cloud.engine.pvm.delegate.ActivityBehavior;

import java.util.Map;

/**
 * @author yurui
 * @Description: 用于查询流程元素对应的抽象行为
 * @Date: 11:49 2019/8/7
 * Copyright (c) huidiancloud
 */
public interface IBehaviorDetecter {

    /**
     * 添加元素行为
     * @param shape
     * @param activityBehavior
     */
    void put(String shape, ActivityBehavior activityBehavior);

    /**
     * 根据元素类型获取行为
     * @param shape
     * @return
     */
    ActivityBehavior getBehavior(String shape);

    /**
     * 配置元素行为
     * @param behaviors
     * @return
     */
    void setBehavior(Map<String, ActivityBehavior> behaviors);

}
