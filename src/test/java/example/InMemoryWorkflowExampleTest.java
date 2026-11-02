package example;

import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessInstance;
import junit.framework.TestCase;

import java.util.Collections;

public class InMemoryWorkflowExampleTest extends TestCase {

    public void testStartWaitSignalAndEnd() {
        PvmProcessInstance instance = InMemoryWorkflowExample
                .createProcessDefinition().createProcessInstance();
        instance.setVariable("requestId", "TEST-001");

        instance.start();

        assertFalse(instance.isEnded());
        assertEquals(Collections.singletonList("wait"), instance.findActiveActivityIds());
        PvmExecution waitingExecution = instance.findExecution("wait");
        assertNotNull(waitingExecution);
        assertEquals("TEST-001", waitingExecution.getVariable("requestId"));

        waitingExecution.signal("continue", "approved");

        assertTrue(instance.isEnded());
        assertTrue(instance.findActiveActivityIds().isEmpty());
        assertEquals("approved", instance.getVariable("result"));
        assertEquals("TEST-001", instance.getVariable("requestId"));
    }

    public void testInvalidSignalKeepsWaitingAndCanRecover() {
        PvmProcessInstance instance = InMemoryWorkflowExample
                .createProcessDefinition().createProcessInstance();
        instance.start();
        PvmExecution waitingExecution = instance.findExecution("wait");

        try {
            waitingExecution.signal("wrong-signal", "must-not-be-saved");
            fail("错误信号应被拒绝");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("continue"));
        }

        assertFalse(instance.isEnded());
        assertEquals(Collections.singletonList("wait"), instance.findActiveActivityIds());
        assertFalse(instance.hasVariable("result"));

        waitingExecution.signal("continue", "approved");

        assertTrue(instance.isEnded());
        assertTrue(instance.findActiveActivityIds().isEmpty());
        assertEquals("approved", instance.getVariable("result"));
    }
}
