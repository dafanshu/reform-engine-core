package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.End;
import pvm.WaitState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NestedScopeInitialTest extends TestCase {
    public void testScopedInitialWait() { verify(1, true, false); }
    public void testNonScopedInitialWait() { verify(1, false, false); }
    public void testDeeperScopedInitialWait() { verify(3, true, false); }
    public void testDeeperNonScopedInitialWait() { verify(3, false, false); }
    public void testTopLevelScopedInitialWait() { verify(0, true, false); }
    public void testExplicitNestedInitialWait() { verify(2, true, true); }

    public void testNonScopeAncestorWithScopedInitialWait() { verify(2, true, false, true); }
    public void testNonScopeAncestorWithNonScopedInitialWait() { verify(2, false, false, true); }

    private void verify(int depth, boolean scopedWait, boolean explicitInitial) {
        verify(depth, scopedWait, explicitInitial, false);
    }

    private void verify(int depth, boolean scopedWait, boolean explicitInitial, boolean nonScopeAncestor) {
        List<String> events = new ArrayList<String>();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("nested")
                .executionListener(PvmEvent.EVENTNAME_START, e -> events.add("process"));
        if (explicitInitial) {
            builder.createActivity("default").initial().behavior(new End()).endActivity();
        }
        List<String> expectedEvents = new ArrayList<String>();
        expectedEvents.add("process");
        if (nonScopeAncestor) {
            builder.createActivity("container")
                    .executionListener(PvmEvent.EVENTNAME_START, e -> events.add("container"));
            expectedEvents.add("container");
        }
        for (int i = 0; i < depth; i++) {
            final String id = "scope" + i;
            builder.createActivity(id).scope()
                    .executionListener(PvmEvent.EVENTNAME_START, e -> events.add(id));
            expectedEvents.add(id);
        }
        builder.createActivity("wait");
        if (scopedWait) builder.scope();
        if (!explicitInitial) builder.initial();
        builder.executionListener(PvmEvent.EVENTNAME_START, e -> events.add("wait"))
                .behavior(new WaitState() {
                    @Override public void execute(com.cloud.engine.pvm.delegate.ActivityExecution execution) {
                        events.add("execute");
                    }
                }).transition("after").endActivity();
        for (int i = 0; i < depth; i++) builder.endActivity();
        if (nonScopeAncestor) builder.endActivity();
        builder.createActivity("after").behavior(new WaitState()).transition("end").endActivity()
                .createActivity("end").behavior(new End()).endActivity();
        ProcessDefinitionImpl definition = (ProcessDefinitionImpl) builder.buildProcessDefinition();
        ExecutionImpl root = (ExecutionImpl) (explicitInitial
                ? definition.createProcessInstanceForInitial(definition.findActivity("wait"))
                : definition.createProcessInstance());
        root.start();
        expectedEvents.addAll(Arrays.asList("wait", "execute"));
        assertEquals(expectedEvents, events);
        assertFalse(root.isEnded());
        assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
        ExecutionImpl leaf = root;
        int scopes = depth + (scopedWait ? 1 : 0);
        for (int i = 0; i < scopes; i++) {
            assertFalse("Scope parents must not remain active", leaf.isActive());
            assertNull(leaf.getActivity());
            assertNull(leaf.getStartingExecution());
            assertEquals(1, leaf.getExecutions().size());
            ExecutionImpl child = leaf.getExecutions().get(0);
            assertSame(leaf, child.getParent());
            assertSame(root, child.getProcessInstance());
            assertTrue(child.isScope());
            leaf = child;
        }
        assertTrue(leaf.isActive());
        assertEquals("wait", leaf.getActivity().getId());
        assertTrue(leaf.getExecutions().isEmpty());
        assertNull(leaf.getStartingExecution());
        assertSame(leaf, root.findExecution("wait"));
        leaf.signal(null, null);
        assertEquals(Collections.singletonList("after"), root.findActiveActivityIds());
        assertTrue(root.isActive());
        assertTrue(root.getExecutions().isEmpty());
        root.signal(null, null);
        assertTrue(root.isEnded());
        assertTrue(root.findActiveActivityIds().isEmpty());
        assertEquals(expectedEvents, events);
    }
}
