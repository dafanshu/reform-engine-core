package acceptance;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.json.JsonModel;
import com.cloud.engine.json.elements.Edge;
import com.cloud.engine.json.elements.Node;
import com.cloud.engine.process.impl.SimpleBehaviorDetecterImpl;
import com.cloud.engine.process.impl.SimpleModelParserImpl;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.CompositeActivityBehavior;
import com.cloud.engine.pvm.delegate.ExecutionListenerExecution;
import com.cloud.engine.pvm.delegate.SignallableActivityBehavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Consumer-owned behaviors; these are examples, not engine-provided task types. */
public final class ApiUsageExamples {
    private ApiUsageExamples() {
    }

    public static void main(String[] args) {
        List<String> events = new ArrayList<>();
        PvmProcessInstance signal = auditedSignal(events).createProcessInstance();
        signal.start();
        signal.findExecution("wait").signal("accept", "fictional-input");
        System.out.println("SIGNAL_RESULT=" + signal.getVariable("result") + " EVENTS=" + events);
        PvmProcessInstance local = localVariables().createProcessInstance();
        local.setVariable("value", "parent");
        local.start();
        System.out.println("LOCAL_PARENT=" + local.getVariable("value"));
        PvmProcessInstance group = composite().createProcessInstance();
        group.start();
        System.out.println("COMPOSITE_COMPLETED=" + group.getVariable("groupCompleted"));
        PvmProcessInstance model = nodeEdgeModel().createProcessInstance();
        model.start();
        System.out.println("MODEL_RESULT=" + model.getVariable("result"));
    }

    public static PvmProcessDefinition auditedSignal(List<String> events) {
        ExecutionListener audit = execution -> {
            ExecutionListenerExecution event = (ExecutionListenerExecution) execution;
            events.add(event.getEventSource().getId() + ":" + event.getEventName());
        };
        return new ProcessDefinitionBuilder("signal-example")
                .setKey("signal-key").setName("信号案例").setDescription("虚构输入处理")
                .property("version", "example-v1")
                .executionListener("start", audit).executionListener("end", audit)
                .createActivity("wait").initial().property("name", "等待输入")
                    .executionListener("start", audit).executionListener("end", audit)
                    .behavior(new SignallableActivityBehavior() {
                        public void execute(ActivityExecution execution) {
                            // Returning without taking a transition is a wait, not a blocked thread.
                        }
                        public void signal(ActivityExecution execution, String name, Object data) {
                            if (!"accept".equals(name) || !(data instanceof String)) {
                                throw new IllegalArgumentException("accept requires a String payload");
                            }
                            execution.setVariable("result", data);
                            execution.take(execution.getActivity().getOutgoingTransitions().get(0));
                        }
                    })
                    .startTransition("done", "accepted").property("purpose", "accepted input")
                        .executionListener(audit).endTransition()
                .endActivity()
                .createActivity("done").behavior(ActivityExecution::end).endActivity()
                .buildProcessDefinition();
    }

    public static PvmProcessDefinition localVariables() {
        return new ProcessDefinitionBuilder("variables-example")
                .createActivity("scope").scope()
                    .createActivity("read").initial().behavior(execution -> {
                        // Root has value=parent; a scoped local can shadow even with null.
                        execution.createVariableLocal("value", null);
                        execution.getProcessInstance().setVariable("nullShadowPresent",
                                execution.hasVariableLocal("value") && execution.getVariable("value") == null);
                        execution.removeVariableLocal("value");
                        execution.setVariable("observedParent", execution.getVariable("value", String.class));
                        execution.setVariable("value", "updated-parent");
                        execution.setVariablesLocal(Collections.singletonMap("scratch", 1));
                        execution.removeVariablesLocal();
                        execution.end();
                    }).endActivity()
                .endActivity().buildProcessDefinition();
    }

    public static PvmProcessDefinition composite() {
        return new ProcessDefinitionBuilder("composite-example")
                .createActivity("group").initial().scope().behavior(new CompositeActivityBehavior() {
                    public void execute(ActivityExecution execution) {
                        execution.executeActivity(execution.getActivity().findActivity("inside"));
                    }
                    public void lastExecutionEnded(ActivityExecution execution) {
                        execution.setVariable("groupCompleted", true);
                        execution.take(execution.getActivity().getOutgoingTransitions().get(0));
                    }
                })
                    .createActivity("inside").behavior(ActivityExecution::end).endActivity()
                    .transition("done")
                .endActivity()
                .createActivity("done").behavior(ActivityExecution::end).endActivity()
                .buildProcessDefinition();
    }

    public static PvmProcessDefinition nodeEdgeModel() {
        Node first = new Node();
        first.setId("first");
        first.setShape("forward");
        Node last = new Node();
        last.setId("last");
        last.setShape("finish");
        Edge edge = new Edge();
        edge.setId("next");
        edge.setSource("first");
        edge.setTarget("last");
        JsonModel model = new JsonModel();
        model.setPrcDefId("model-example");
        model.setProcKey("model-key");
        model.setProcName("点线案例");
        model.setNodes(Arrays.asList(first, last)); // Parser selects the first node as initial.
        model.setEdges(Collections.singletonList(edge));
        SimpleBehaviorDetecterImpl behaviors = new SimpleBehaviorDetecterImpl();
        behaviors.put("forward", execution -> {
            execution.setVariable("visited", execution.getCurrentActivityId());
            execution.take(execution.getActivity().getOutgoingTransitions().get(0));
        });
        behaviors.put("finish", execution -> {
            execution.setVariable("result", "parsed");
            execution.end();
        });
        return new SimpleModelParserImpl().loads(model, behaviors);
    }
}
