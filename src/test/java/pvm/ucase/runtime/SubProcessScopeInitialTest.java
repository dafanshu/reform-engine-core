package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.SubProcessActivityBehavior;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.End;
import pvm.WaitState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubProcessScopeInitialTest extends TestCase {
    public void testScopedInitialThroughSubProcessEntry() { verify(0, true); }
    public void testNestedScopedInitialThroughSubProcessEntry() { verify(2, true); }
    public void testNestedNonScopedInitialThroughSubProcessEntry() { verify(2, false); }
    public void testNonScopedInitialThroughSubProcessEntry() { verify(0, false); }

    private void verify(int depth, boolean scopedWait) {
        final List<String> events = new ArrayList<String>();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("child");
        for (int i = 0; i < depth; i++) builder.createActivity("scope" + i).scope();
        builder.createActivity("wait").initial();
        if (scopedWait) builder.scope();
        builder.behavior(new WaitState() {
            @Override public void execute(ActivityExecution execution) { events.add("wait"); }
        }).transition("after").endActivity();
        for (int i = 0; i < depth; i++) builder.endActivity();
        PvmProcessDefinition definition = builder.createActivity("after").behavior(new WaitState())
                .transition("end").endActivity().createActivity("end").behavior(new End())
                .endActivity().buildProcessDefinition();
        ProcessDefinitionImpl parentDefinition = new ProcessDefinitionImpl("parent");
        TrackingExecution caller = new TrackingExecution();
        caller.setProcessDefinition(parentDefinition);
        caller.setProcessInstance(caller);
        caller.setActivity(parentDefinition.createActivity("call"));
        caller.getActivity().setActivityBehavior(new SubProcessActivityBehavior() {
            @Override public void execute(ActivityExecution execution) { }
            @Override public void completing(DelegateExecution execution, DelegateExecution child) {
                events.add("completing");
            }
            @Override public void completed(ActivityExecution execution) { events.add("completed"); }
        });
        ExecutionImpl child = (ExecutionImpl) caller.createSubProcessInstance(definition);
        assertSame(caller, child.getSuperExecution());
        assertSame(child, caller.getSubProcessInstance());
        assertNull(child.getParent());
        child.start();
        assertEquals(Collections.singletonList("wait"), events);
        assertEquals(Collections.singletonList("wait"), child.findActiveActivityIds());
        ExecutionImpl leaf = child;
        int scopeCount = depth + (scopedWait ? 1 : 0);
        for (int i = 0; i <= scopeCount; i++) {
            assertTrue("Preserve the execution subclass factory", leaf instanceof TrackingExecution);
            assertEquals("Initialize each scope exactly once", 1, ((TrackingExecution) leaf).initializations);
            assertSame(definition, leaf.getProcessDefinition());
            assertSame(child, leaf.getProcessInstance());
            assertNull(leaf.getStartingExecution());
            if (i < scopeCount) {
                assertFalse(leaf.isActive());
                assertNull(leaf.getActivity());
                assertEquals(1, leaf.getExecutions().size());
                ExecutionImpl next = leaf.getExecutions().get(0);
                assertSame(leaf, next.getParent());
                leaf = next;
            }
        }
        assertTrue(leaf.isScope());
        assertTrue(leaf.isActive());
        assertTrue(leaf.getExecutions().isEmpty());
        assertSame(leaf, child.findExecution("wait"));
        leaf.signal(null, null);
        assertEquals(Collections.singletonList("after"), child.findActiveActivityIds());
        assertTrue(child.isActive());
        assertTrue(child.getExecutions().isEmpty());
        child.signal(null, null);
        assertTrue(child.isEnded());
        assertTrue(child.findActiveActivityIds().isEmpty());
        assertNull(caller.getSubProcessInstance());
        assertEquals(Arrays.asList("wait", "completing", "completed"), events);
    }

    private static class TrackingExecution extends ExecutionImpl {
        private int initializations;
        @Override protected ExecutionImpl newExecution() { return new TrackingExecution(); }
        @Override public void initialize() {
            assertNotNull(getProcessDefinition());
            assertNotNull(getProcessInstance());
            initializations++;
        }
    }
}
