package com.cloud.engine.pvm;

import com.cloud.engine.delegate.Expression;

public interface PvmTransition extends PvmProcessElement {

  /**
   * 获取起始环节
   * @return
   */
  PvmActivity getSource();

  /**
   * 获取结束环节
   * @return
   */
  PvmActivity getDestination();

  Expression getSkipExpression();
}
