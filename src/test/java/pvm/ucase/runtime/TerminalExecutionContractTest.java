package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class TerminalExecutionContractTest extends TestCase {
    private static class CountingWait extends WaitState {
        int signals;
        @Override public void signal(ActivityExecution e, String name, Object data) { signals++; super.signal(e, name, data); }
    }
    public void testNaturalEndRejectsSignalBeforeBehaviorRuns() { terminal(false, false); }
    public void testScopedNaturalEndRejectsSignalBeforeActivityLookup() { terminal(true, false); }
    public void testWholeProcessCancellationIsTerminal() { terminal(false, true); }
    public void testScopedWholeProcessCancellationIsTerminal() { terminal(true, true); }
    private void terminal(boolean scoped, boolean cancel) {
        CountingWait wait = new CountingWait(); List<ExecutionImpl> events = new ArrayList<ExecutionImpl>();
        ProcessDefinitionBuilder b = new ProcessDefinitionBuilder("terminal")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add((ExecutionImpl)e));
        if (scoped) b.createActivity("scope").scope();
        b.createActivity("wait").initial().behavior(wait).endActivity();
        if (scoped) b.endActivity();
        ExecutionImpl root = (ExecutionImpl)b.buildProcessDefinition().createProcessInstance(); root.start();
        if (cancel) root.deleteCascade("cancel whole process"); else root.findExecution("wait").signal(null, null);
        assertTrue(root.isEnded()); assertFalse(root.isActive());
        assertTrue(root.getExecutions().isEmpty()); assertTrue(root.findActiveActivityIds().isEmpty());
        assertEquals(Collections.singletonList(root), events);
        int before = wait.signals;
        rejectSignal(root);
        assertEquals(before, wait.signals); assertEquals(Collections.singletonList(root), events);
    }
    private void rejectSignal(ExecutionImpl execution) {
        try { execution.signal(null, null); fail("ended execution must reject signal"); }
        catch (PvmException expected) { assertTrue(expected.getMessage().contains("ended")); }
    }

    public void testInternalScopeCleanupLeavesOuterProcessRunnable() {
        List<String> events = new ArrayList<String>();
        ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("internal")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("process"))
                .createActivity("outer").scope()
                    .createActivity("inner").scope()
                        .createActivity("wait").initial().behavior(new WaitState()).endActivity()
                    .endActivity()
                    .createActivity("continued").behavior(new WaitState()).endActivity()
                .endActivity().buildProcessDefinition().createProcessInstance();
        root.start();
        ExecutionImpl inner = root.findExecution("wait"), outer = inner.getParent();
        assertNotSame(root, outer); assertTrue(outer.isScope());
        outer.destroyScope("interrupt inner");
        assertTrue(outer.getExecutions().isEmpty()); assertSame(outer, root.getExecutions().get(0));
        assertFalse(outer.isEnded()); assertFalse(root.isEnded()); assertTrue(events.isEmpty());
        outer.setActive(true); outer.executeActivity(root.getProcessDefinition().findActivity("continued"));
        assertEquals(Collections.singletonList("continued"), root.findActiveActivityIds());
        root.findExecution("continued").signal(null, null);
        assertTrue(root.isEnded()); assertEquals(Collections.singletonList("process"), events);
    }

    public void testCancelledCalledProcessIsTerminalWithoutNormalCallbacks() { subprocess(true); }
    public void testNaturallyEndedCalledProcessRejectsFurtherSignal() { subprocess(false); }
    public void testWholeParentCancellationTerminatesCalledProcessToo() { subprocess(true, true); }
    private void subprocess(boolean cancel) { subprocess(cancel, false); }
    private void subprocess(boolean cancel, boolean cancelParent) {
        List<String> events = new ArrayList<String>(); List<ExecutionImpl> children = new ArrayList<ExecutionImpl>();
        PvmProcessDefinition childDefinition = new ProcessDefinitionBuilder("child")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("child-end"))
                .createActivity("childScope").scope()
                    .createActivity("childWait").initial().behavior(new WaitState()).endActivity()
                .endActivity().buildProcessDefinition();
        ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("parent")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("parent-end"))
                .createActivity("call").initial().behavior(new SubProcessActivityBehavior() {
                    public void execute(ActivityExecution e) {
                        ExecutionImpl child = (ExecutionImpl)e.createSubProcessInstance(childDefinition); children.add(child); child.start();
                    }
                    public void completing(DelegateExecution e, DelegateExecution child) { events.add("completing"); }
                    public void completed(ActivityExecution e) { events.add("completed"); e.end(); }
                }).endActivity()
                .createActivity("continued").behavior(new WaitState()).endActivity()
                .buildProcessDefinition().createProcessInstance();
        root.start(); ExecutionImpl child = children.get(0);
        if (cancel) {
            if (cancelParent) {
                root.deleteCascade("cancel parent and child");
                assertTrue(root.isEnded()); assertFalse(root.isActive());
                assertEquals(Arrays.asList("child-end", "parent-end"), events);
                rejectSignal(root);
            } else {
                root.destroyScope("cancel call");
                assertFalse(root.isEnded()); assertEquals(Collections.singletonList("child-end"), events);
            }
        } else child.findExecution("childWait").signal(null, null);
        assertTrue(child.isEnded()); assertFalse(child.isActive());
        List<String> before = new ArrayList<String>(events); rejectSignal(child); assertEquals(before, events);
        if (cancel) {
            if (!cancelParent) {
                root.setActive(true); root.executeActivity(root.getProcessDefinition().findActivity("continued")); root.signal(null, null);
            }
            assertEquals(Arrays.asList("child-end", "parent-end"), events);
        } else assertEquals(Arrays.asList("child-end", "completing", "completed", "parent-end"), events);
        assertTrue(root.isEnded());
    }

    public void testFailedCancellationDoesNotClaimSuccessfulTermination() {
        IllegalStateException failure = new IllegalStateException("listener failed");
        ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("failure")
                .executionListener(PvmEvent.EVENTNAME_END, e -> { e.setVariable("observed", true); throw failure; })
                .createActivity("wait").initial().behavior(new WaitState()).endActivity()
                .buildProcessDefinition().createProcessInstance(); root.start();
        try {root.deleteCascade("cancel"); fail();} catch (IllegalStateException actual) {assertSame(failure, actual);}
        assertFalse(root.isEnded()); assertEquals(true, root.getVariable("observed"));
        // Cleanup and listener side effects before the failure are not rolled back.
    }

    public void testRepeatedSignalsWhileWaitingRemainValid() {
        final int[] calls = {0};
        ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("collect")
                .createActivity("collect").initial().behavior(new SignallableActivityBehavior() {
                    public void execute(ActivityExecution e) { }
                    public void signal(ActivityExecution e, String name, Object data) {if (++calls[0] == 2) e.end();}
                }).endActivity().buildProcessDefinition().createProcessInstance(); root.start();
        root.signal("piece", null); assertFalse(root.isEnded()); root.signal("piece", null);
        assertTrue(root.isEnded()); assertEquals(2, calls[0]);
    }
}
