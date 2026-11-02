package example;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.SignallableActivityBehavior;

/**
 * 纯内存示例：start -> wait --continue--> end。
 * 直接运行 main；进程退出后，流程实例和变量不会保存。
 */
public class InMemoryWorkflowExample {

    public static void main(String[] args) {
        PvmProcessInstance instance = createProcessDefinition().createProcessInstance();
        instance.setVariable("requestId", "DEMO-001");

        System.out.println("[1/4] 启动流程，requestId=" + instance.getVariable("requestId"));
        instance.start();

        // start() 已经返回；等待只是保留当前位置，不会阻塞调用线程。
        System.out.println("[2/4] 等待信号，active=" + instance.findActiveActivityIds()
                + ", ended=" + instance.isEnded());
        PvmExecution waitingExecution = instance.findExecution("wait");
        if (waitingExecution == null || instance.isEnded()) {
            throw new IllegalStateException("流程没有停留在等待节点");
        }

        // 模拟稍后收到按钮操作或外部回调，在同一个内存实例上继续执行。
        System.out.println("[3/4] 接收信号，name=continue, data=approved");
        waitingExecution.signal("continue", "approved");

        System.out.println("[4/4] 流程结束，active=" + instance.findActiveActivityIds()
                + ", ended=" + instance.isEnded()
                + ", result=" + instance.getVariable("result"));
    }

    static PvmProcessDefinition createProcessDefinition() {
        return new ProcessDefinitionBuilder("in-memory-example")
                .createActivity("start")
                    .initial()
                    .behavior(execution -> execution.take(
                            execution.getActivity().getOutgoingTransitions().get(0)))
                    .transition("wait")
                .endActivity()
                .createActivity("wait")
                    .behavior(new SignallableActivityBehavior() {
                        @Override
                        public void execute(ActivityExecution execution) {
                            // 不调用 take() 或 end()，流程就停在这里。
                        }

                        @Override
                        public void signal(ActivityExecution execution,
                                           String signalName, Object signalData) {
                            // 先校验信号，避免错误信号误结束流程或写入结果。
                            if (!"continue".equals(signalName)) {
                                throw new IllegalArgumentException(
                                        "不支持的信号：" + signalName + "，应为 continue");
                            }
                            execution.setVariable("result", signalData);
                            execution.take(execution.getActivity()
                                    .findOutgoingTransition("continue"));
                        }
                    })
                    .transition("end", "continue")
                .endActivity()
                .createActivity("end")
                    .behavior(execution -> execution.end())
                .endActivity()
                .buildProcessDefinition();
    }
}
