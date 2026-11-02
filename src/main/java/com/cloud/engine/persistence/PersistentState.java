package com.cloud.engine.persistence;

import lombok.Data;

import java.util.Objects;

/**
 * @author yurui
 * @Description:  管理Execution的可持久化字段
 * @Date: 9:43 2019/8/12
 * Copyright (c) huidiancloud
*/
@Data
public class PersistentState {
    private String  id;
    private String  processDefinitionId;
    private String  businessKey;
    private String  activityId;
    private boolean isActive = true;
    private boolean isScope = true;
    private boolean isConcurrent = false;
    private boolean isEnded = false;
    private boolean isEventScope = false;
    private String  parentId;
    private String  name;
    private String  superExecutionId;
    private String processInstanceId;

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        if (!super.equals(o)){
            return false;
        }
        PersistentState that = (PersistentState) o;
        return isActive == that.isActive &&
                isScope == that.isScope &&
                isConcurrent == that.isConcurrent &&
                isEnded == that.isEnded &&
                isEventScope == that.isEventScope &&
                Objects.equals(id, that.id) &&
                Objects.equals(processDefinitionId, that.processDefinitionId) &&
                Objects.equals(businessKey, that.businessKey) &&
                Objects.equals(activityId, that.activityId) &&
                Objects.equals(parentId, that.parentId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(superExecutionId, that.superExecutionId) &&
                Objects.equals(processInstanceId, that.processInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, processDefinitionId, businessKey, activityId, isActive, isScope, isConcurrent, isEnded, isEventScope, parentId, name, superExecutionId, processInstanceId);
    }
}
