package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import com.cloud.engine.pvm.runtime.InterpretableExecution;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class LazyScopeStartTest extends TestCase {
    public void testLazyScopedInitial() { verify(false, false, false); }
    public void testDirectScopedInitial() { verify(true, false, false); }
    public void testLazyNestedInitial() { verify(false, true, false); }
    public void testLazyStartListenerRetryDoesNotInitializeTwice() { verify(false, true, true); }

    private void verify(boolean eager, boolean nested, boolean retryListener) {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger attempts = new AtomicInteger();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("lazy");
        builder.executionListener(PvmEvent.EVENTNAME_START, e -> {
            if (attempts.incrementAndGet() == 1 && retryListener) throw new IllegalStateException("retry");
        });
        if (nested) builder.createActivity("outer").scope();
        builder.createActivity("wait").scope().initial().behavior(new WaitState() {
            @Override public void execute(com.cloud.engine.pvm.delegate.ActivityExecution execution) {
                calls.incrementAndGet();
            }
        }).endActivity();
        if (nested) builder.endActivity();
        final ProcessDefinitionImpl definition = (ProcessDefinitionImpl) builder.buildProcessDefinition();
        CountingExecution root = new CountingExecution() {
            @Override protected void ensureProcessDefinitionInitialized() {
                if (processDefinition == null) setProcessDefinition(definition);
            }
        };
        if (eager) root.setProcessDefinition(definition);
        root.setProcessInstance(root);
        if (retryListener) {
            try { root.start(); fail("Listener failure must propagate"); }
            catch (IllegalStateException expected) { assertEquals("retry", expected.getMessage()); }
            assertEquals(0, calls.get());
            assertTree(root, nested ? 2 : 1);
        }
        root.start();
        assertTree(root, nested ? 2 : 1);
        assertEquals(1, calls.get());
        assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
        assertNull(root.getStartingExecution());
    }

    public void testFactoryInitializationIsNotRepeatedOnStart() {
        ProcessDefinitionImpl definition = new ProcessDefinitionImpl("factory") {
            @Override protected InterpretableExecution newProcessInstance(ActivityImpl initial) {
                return new CountingExecution(initial);
            }
        };
        ActivityImpl wait = definition.createActivity("wait");
        wait.setScope(true);
        wait.setActivityBehavior(new WaitState());
        definition.setInitial(wait);
        CountingExecution root = (CountingExecution) definition.createProcessInstance();
        assertTree(root, 1);
        ExecutionImpl child = root.getExecutions().get(0);
        root.start();
        assertTree(root, 1);
        assertSame(child, root.getExecutions().get(0));
        assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
    }

    public void testDirectExplicitInitialKeepsSelectedActivity() {
        ProcessDefinitionImpl definition = new ProcessDefinitionImpl("explicit");
        ActivityImpl defaultActivity = definition.createActivity("default");
        definition.setInitial(defaultActivity);
        ActivityImpl chosen = definition.createActivity("wait");
        chosen.setScope(true);
        chosen.setActivityBehavior(new WaitState());
        CountingExecution root = new CountingExecution(chosen);
        root.setProcessDefinition(definition);
        root.start();
        assertTree(root, 1);
        assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
    }

    private void assertTree(CountingExecution root, int scopes) {
        ExecutionImpl current = root;
        for (int i = 0; i <= scopes; i++) {
            assertEquals(1, ((CountingExecution) current).initializations);
            assertSame(root, current.getProcessInstance());
            assertSame(root.getProcessDefinition(), current.getProcessDefinition());
            if (i < scopes) {
                assertFalse(current.isActive());
                assertNull(current.getActivity());
                assertEquals(1, current.getExecutions().size());
                ExecutionImpl child = current.getExecutions().get(0);
                assertSame(current, child.getParent());
                current = child;
            }
        }
        assertTrue(current.isActive());
        assertEquals("wait", current.getActivity().getId());
        assertTrue(current.getExecutions().isEmpty());
    }

    private static class CountingExecution extends ExecutionImpl {
        private int initializations;
        CountingExecution() { }
        CountingExecution(ActivityImpl initial) { super(initial); }
        @Override protected ExecutionImpl newExecution() { return new CountingExecution(); }
        @Override public void initialize() { initializations++; }
    }
}
