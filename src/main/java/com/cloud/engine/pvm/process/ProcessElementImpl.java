package com.cloud.engine.pvm.process;

import com.cloud.engine.pvm.PvmProcessElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProcessElementImpl implements PvmProcessElement {

  private static final long serialVersionUID = 1L;

  protected String id;
  protected ProcessDefinitionImpl processDefinition;
  protected Map<String, Object> properties;

  public ProcessElementImpl(String id, ProcessDefinitionImpl processDefinition) {
    this.id = id;
    this.processDefinition = processDefinition;
  }

  public void setProperty(String name, Object value) {
    if (properties==null) {
      properties = new HashMap<>();
    }
    properties.put(name, value);
  }

  @Override
  public Object getProperty(String name) {
    if (properties==null) {
      return null;
    }
    return properties.get(name);
  }

  public Map<String, Object> getProperties() {
    if (properties==null) {
      return Collections.EMPTY_MAP;
    }
    return properties;
  }

  @Override
  public String getId() {
    return id;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public ProcessDefinitionImpl getProcessDefinition() {
    return processDefinition;
  }
}
