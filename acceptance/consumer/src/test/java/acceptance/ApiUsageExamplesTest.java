package acceptance;

import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ApiUsageExamplesTest {
    @Test
    public void listenersMetadataAndSignalPayload() {
        List<String> events = new ArrayList<>();
        PvmProcessDefinition definition = ApiUsageExamples.auditedSignal(events);
        assertEquals("signal-key", definition.getKey());
        assertEquals("信号案例", definition.getName());
        assertEquals("example-v1", definition.getProperty("version"));
        assertEquals("等待输入", definition.findActivity("wait").getProperty("name"));
        assertEquals("accepted input", definition.findActivity("wait")
                .getOutgoingTransitions().get(0).getProperty("purpose"));
        PvmProcessInstance instance = definition.createProcessInstance();
        instance.start();
        assertFalse(instance.isEnded());
        assertEquals(Arrays.asList("signal-example:start", "wait:start"), events);
        try {
            instance.findExecution("wait").signal("unknown", "payload");
            fail("Consumer rejects unknown signal before changing data");
        } catch (IllegalArgumentException expected) {
            assertNull(instance.getVariable("result"));
            assertFalse(instance.isEnded());
        }
        instance.findExecution("wait").signal("accept", "payload");
        assertTrue(instance.isEnded());
        assertEquals("payload", instance.getVariable("result"));
        assertEquals(Arrays.asList("signal-example:start", "wait:start", "wait:end",
                "accepted:take", "signal-example:end"), events);
    }

    @Test
    public void cancellationAlsoNotifiesEndButDoesNotProduceResult() {
        List<String> events = new ArrayList<>();
        PvmProcessInstance instance = ApiUsageExamples.auditedSignal(events).createProcessInstance();
        instance.start();
        instance.deleteCascade("caller abort");
        assertTrue(instance.isEnded());
        assertTrue(instance.findActiveActivityIds().isEmpty());
        assertNull(instance.getVariable("result"));
        assertEquals(Arrays.asList("signal-example:start", "wait:start", "wait:end",
                "signal-example:end"), events);
    }

    @Test
    public void scopedLocalShadowAndParentUpdate() {
        PvmProcessInstance instance = ApiUsageExamples.localVariables().createProcessInstance();
        instance.setVariable("value", "parent");
        instance.start();
        assertTrue(instance.isEnded());
        assertEquals(true, instance.getVariable("nullShadowPresent"));
        assertEquals("parent", instance.getVariable("observedParent"));
        assertEquals("updated-parent", instance.getVariable("value"));
        assertFalse(instance.hasVariable("scratch"));
    }

    @Test
    public void compositeCompletionContinuesOutsideScope() {
        PvmProcessInstance instance = ApiUsageExamples.composite().createProcessInstance();
        instance.start();
        assertTrue(instance.isEnded());
        assertEquals(true, instance.getVariable("groupCompleted"));
    }

    @Test
    public void parsesFlatNodeEdgeModelWithExplicitBehaviorRegistry() {
        PvmProcessDefinition definition = ApiUsageExamples.nodeEdgeModel();
        assertEquals("first", definition.getInitial().getId());
        assertEquals("model-key", definition.getKey());
        PvmProcessInstance instance = definition.createProcessInstance();
        instance.start();
        assertTrue(instance.isEnded());
        assertEquals("first", instance.getVariable("visited"));
        assertEquals("parsed", instance.getVariable("result"));
    }
}
