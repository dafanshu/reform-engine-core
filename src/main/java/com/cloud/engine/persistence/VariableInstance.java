
package com.cloud.engine.persistence;
/**
 * @author yurui
 * @Description: 这里暂时保留吧，主要是关于流程参数方面的内容目前还没有很好的落点
 * @Date: 16:31 2019/8/23
 * Copyright (c) huidiancloud
*/
public interface VariableInstance  {

  void setName(String name);

  String getLocalizedName();

  void setLocalizedName(String name);

  String getLocalizedDescription();

  void setLocalizedDescription(String description);

  void setProcessInstanceId(String processInstanceId);

  void setExecutionId(String executionId);

  Object getValue();

  void setValue(Object value);

  String getTypeName();

  void setTypeName(String typeName);

  String getProcessInstanceId();

  String getTaskId();

  void setTaskId(String taskId);

  String getExecutionId();

}