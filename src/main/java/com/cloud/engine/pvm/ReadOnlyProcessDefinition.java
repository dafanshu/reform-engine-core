package com.cloud.engine.pvm;
public interface ReadOnlyProcessDefinition extends PvmScope {

  String getName();

  String getKey();

  String getDescription();

  PvmActivity getInitial();

}
