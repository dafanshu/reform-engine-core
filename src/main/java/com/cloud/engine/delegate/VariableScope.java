package com.cloud.engine.delegate;

import com.cloud.engine.persistence.VariableInstance;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
/**
 * @author yurui
 * @Description: 这里暂时保留吧，主要是关于流程参数方面的内容目前还没有很好的落点
 * @Date: 16:32 2019/8/23
 * Copyright (c) huidiancloud
*/
public interface VariableScope {

  Map<String, Object> getVariables();

  Map<String, VariableInstance> getVariableInstances();

  Map<String, Object> getVariables(Collection<String> variableNames);

  Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames);

  Map<String, Object> getVariables(Collection<String> variableNames, boolean fetchAllVariables);

  Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames, boolean fetchAllVariables);

  Map<String, Object> getVariablesLocal();

  Map<String, VariableInstance> getVariableInstancesLocal();

  Map<String, Object> getVariablesLocal(Collection<String> variableNames);

  Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames);

  Map<String, Object> getVariablesLocal(Collection<String> variableNames, boolean fetchAllVariables);

  Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames, boolean fetchAllVariables);

  Object getVariable(String variableName);

  VariableInstance getVariableInstance(String variableName);

  Object getVariable(String variableName, boolean fetchAllVariables);

  VariableInstance getVariableInstance(String variableName, boolean fetchAllVariables);

  Object getVariableLocal(String variableName);

  VariableInstance getVariableInstanceLocal(String variableName);

  Object getVariableLocal(String variableName, boolean fetchAllVariables);

  VariableInstance getVariableInstanceLocal(String variableName, boolean fetchAllVariables);

  <T> T getVariable(String variableName, Class<T> variableClass);

  <T> T getVariableLocal(String variableName, Class<T> variableClass);

  Set<String> getVariableNames();

  Set<String> getVariableNamesLocal();

  void setVariable(String variableName, Object value);

  void setVariable(String variableName, Object value, boolean fetchAllVariables);

  Object setVariableLocal(String variableName, Object value);

  Object setVariableLocal(String variableName, Object value, boolean fetchAllVariables);

  void setVariables(Map<String, ? extends Object> variables);

  void setVariablesLocal(Map<String, ? extends Object> variables);

  boolean hasVariables();

  boolean hasVariablesLocal();

  boolean hasVariable(String variableName);

  boolean hasVariableLocal(String variableName);

  void createVariableLocal(String variableName, Object value);

  void removeVariable(String variableName);

  void removeVariableLocal(String variableName);

  void removeVariables(Collection<String> variableNames);

  void removeVariablesLocal(Collection<String> variableNames);

  void removeVariables();

  void removeVariablesLocal();

}