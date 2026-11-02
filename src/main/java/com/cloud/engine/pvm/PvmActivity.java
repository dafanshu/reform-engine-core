package com.cloud.engine.pvm;

import java.util.List;

public interface PvmActivity extends PvmScope {

  boolean isAsync();

  boolean isExclusive();

  PvmScope getParent();

  List<PvmTransition> getIncomingTransitions();

  List<PvmTransition> getOutgoingTransitions();

  PvmTransition findOutgoingTransition(String transitionId);
}
