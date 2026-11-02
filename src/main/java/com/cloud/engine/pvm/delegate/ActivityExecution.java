package com.cloud.engine.pvm.delegate;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.PvmActivity;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.PvmTransition;
import java.util.List;

public interface ActivityExecution extends DelegateExecution {

  /**
   * 获取当前执行环节
   */
  PvmActivity getActivity();

  /**
   * 通过执行给定线路离开当前环节
   */
  void take(PvmTransition transition);


  /**
   * 新建执行器
   */
  ActivityExecution createExecution();

  /**
   * 创建一个子流程实例
   */
  PvmProcessInstance createSubProcessInstance(PvmProcessDefinition processDefinition);

  /**
   * 返回当前执行器的父级，如果没有就返回null
   */
  ActivityExecution getParent();

  ActivityExecution getProcessInstance();

  /**
   * returns the list of execution of which this execution the parent of.
   */
  List<? extends ActivityExecution> getExecutions();

  /**
   * 结束当前执行器
   */
  void end();


  /* State management */

  /**
   * makes this execution active or inactive.
   */
  void setActive(boolean isActive);

  /**
   * returns whether this execution is currently active.
   */
  boolean isActive();

  /**
   * returns whether this execution has ended or not.
   */
  boolean isEnded();

  /**
   * Sets whether this execution is ended or not.
   * Note that this won't trigger any deletion or such, it simply sets the boolean.
   */
  void setEnded(boolean ended);

  /**
   * changes the concurrent indicator on this execution.
   */
  void setConcurrent(boolean isConcurrent);

  /**
   * returns whether this execution is concurrent or not.
   */
  boolean isConcurrent();

  /**
   * returns whether this execution is a process instance or not.
   */
  boolean isProcessInstanceType();

  /**
   * Inactivates this execution.
   * This is useful for example in a join: the execution
   * still exists, but it is not longer active.
   */
  void inactivate();

  /**
   * Returns whether this execution is a scope.
   */
  boolean isScope();

  /**
   * Changes whether this execution is a scope or not
   */
  void setScope(boolean isScope);

  /**
   * Retrieves all executions which are concurrent and inactive at the given activity.
   */
  List<ActivityExecution> findInactiveConcurrentExecutions(PvmActivity activity);

  /**
   * Takes the given outgoing transitions, and potentially reusing
   * the given list of executions that were previously joined.
   */
  void takeAll(List<PvmTransition> outgoingTransitions, List<ActivityExecution> joinedExecutions);

  /**
   * Executes the {@link ActivityBehavior} associated with the given activity.
   */
  void executeActivity(PvmActivity activity);

  /**
   * Called when an execution is interrupted.
   *
   * Performs destroy scope behavior: all child executions and sub-process instances and other related
   * resources are removed. The execution itself can continue execution.
   */
  void destroyScope(String reason);
}
