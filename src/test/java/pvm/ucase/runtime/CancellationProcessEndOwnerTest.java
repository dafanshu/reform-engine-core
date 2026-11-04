package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class CancellationProcessEndOwnerTest extends TestCase {
    public void testDestroySingleScopeDoesNotEndProcess() { verify(1, "destroy"); }
    public void testDestroyNestedScopesDoesNotEndProcess() { verify(3, "destroy"); }
    public void testDeleteSingleScopeProcessNotifiesOnlyRoot() { verify(1, "delete"); }
    public void testDeleteNestedScopeProcessNotifiesOnlyRoot() { verify(3, "delete"); }
    public void testDeleteUnscopedProcessStillNotifiesRoot() { verify(0, "delete"); }
    public void testNaturalSingleScopeEndStillNotifiesRoot() { verify(1, "natural"); }
    public void testNaturalNestedScopeEndStillNotifiesRoot() { verify(3, "natural"); }

    private void verify(int depth, String mode) {
        List<String> activities = new ArrayList<String>();
        List<ExecutionImpl> processEvents = new ArrayList<ExecutionImpl>();
        ProcessDefinitionBuilder b = new ProcessDefinitionBuilder("p")
                .executionListener(PvmEvent.EVENTNAME_END, e -> {
                    ExecutionImpl current = (ExecutionImpl)e;
                    assertSame(current.getProcessDefinition(), current.getEventSource());
                    processEvents.add(current);
                });
        for (int i = 0; i < depth; i++) {
            final String name = "scope" + i;
            b.createActivity(name).scope().executionListener(PvmEvent.EVENTNAME_END, e -> activities.add(name));
        }
        b.createActivity("wait").initial().behavior(new WaitState())
                .executionListener(PvmEvent.EVENTNAME_END, e -> activities.add("wait")).endActivity();
        for (int i = 0; i < depth; i++) b.endActivity();
        b.createActivity("continued").behavior(new WaitState()).endActivity();
        ExecutionImpl root = (ExecutionImpl)b.buildProcessDefinition().createProcessInstance();
        root.start();
        if ("destroy".equals(mode)) {
            root.destroyScope("interrupt scope");
            assertFalse(root.isEnded());
            assertTrue(root.getExecutions().isEmpty());
            assertTrue("scope cleanup must not notify process end", processEvents.isEmpty());
            root.setActive(true);
            root.executeActivity(root.getProcessDefinition().findActivity("continued"));
            assertEquals(Collections.singletonList("continued"), root.findActiveActivityIds());
            assertTrue(processEvents.isEmpty());
            root.signal(null, null);
        } else if ("delete".equals(mode)) {
            root.deleteCascade("cancel process");
        } else {
            root.findExecution("wait").signal(null, null);
        }
        List<String> expectedActivities = new ArrayList<String>();
        expectedActivities.add("wait");
        for (int i = depth - 1; i >= 0; i--) expectedActivities.add("scope" + i);
        assertEquals(expectedActivities, activities);
        assertEquals("one notification owned by the process root", Collections.singletonList(root), processEvents);
        assertTrue(root.getExecutions().isEmpty());
        if (!"delete".equals(mode)) {
            assertTrue(root.isEnded()); assertFalse(root.isActive());
        }
    }

    public void testCancellingNestedSubprocessNotifiesChildRootOnly() {
        List<ExecutionImpl> childEvents = new ArrayList<ExecutionImpl>();
        List<ExecutionImpl> parentEvents = new ArrayList<ExecutionImpl>();
        List<String> callbacks = new ArrayList<String>();
        PvmProcessDefinition childDefinition = new ProcessDefinitionBuilder("child")
                .executionListener(PvmEvent.EVENTNAME_END, e -> childEvents.add((ExecutionImpl)e))
                .createActivity("childScope").scope()
                    .createActivity("childWait").initial().behavior(new WaitState()).endActivity()
                .endActivity().buildProcessDefinition();
        ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("parent")
                .executionListener(PvmEvent.EVENTNAME_END, e -> parentEvents.add((ExecutionImpl)e))
                .createActivity("parentScope").scope()
                    .createActivity("call").initial().behavior(new SubProcessActivityBehavior() {
                        public void execute(ActivityExecution e) { e.createSubProcessInstance(childDefinition).start(); }
                        public void completing(DelegateExecution e, DelegateExecution child) { callbacks.add("completing"); }
                        public void completed(ActivityExecution e) { callbacks.add("completed"); e.end(); }
                    }).endActivity()
                .endActivity().buildProcessDefinition().createProcessInstance();
        root.start();
        ExecutionImpl child = root.findExecution("call").getSubProcessInstance();
        root.destroyScope("cancel work");
        assertEquals(Collections.singletonList(child), childEvents);
        assertTrue(parentEvents.isEmpty()); assertTrue(callbacks.isEmpty());
        assertTrue(root.getExecutions().isEmpty()); assertTrue(child.getExecutions().isEmpty());
        assertFalse(root.isEnded());
    }
}
