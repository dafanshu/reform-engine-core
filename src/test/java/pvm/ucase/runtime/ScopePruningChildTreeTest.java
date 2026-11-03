package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.SubProcessActivityBehavior;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class ScopePruningChildTreeTest extends TestCase {
    public void testScopeExitPreservesWaitingChild() { verify(true, 0, false, false); }
    public void testOrdinaryEndPreservesWaitingChild() { verify(false, 0, false, false); }
    public void testScopeExitPreservesDeepTree() { verify(true, 2, false, false); }
    public void testOrdinaryEndPreservesDeepTree() { verify(false, 2, false, false); }
    public void testScopedSiblingCompletesBeforeScopeExit() { verify(true, 2, true, false); }
    public void testScopedSiblingCompletesBeforeOrdinaryEnd() { verify(false, 2, true, false); }
    public void testScopeExitPreservesChildSubProcess() { verify(true, 2, false, true); }
    public void testOrdinaryEndPreservesChildSubProcess() { verify(false, 2, false, true); }

    public void testOutsideBranchCompletesBeforePreservedTree() { verify(true, 2, false, false, true); }

    private void verify(boolean exits, int depth, boolean childFirst, boolean subprocess) {
        verify(exits, depth, childFirst, subprocess, false);
    }

    private void verify(boolean exits, int depth, boolean childFirst, boolean subprocess, boolean afterFirst) {
        List<ActivityExecution> callbacks = new ArrayList<ActivityExecution>();
        PvmProcessDefinition childDefinition = new ProcessDefinitionBuilder("child")
                .createActivity("childWait").initial().behavior(new WaitState()).endActivity()
                .buildProcessDefinition();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("p")
                .createActivity("outer").scope()
                .createActivity("fork").initial().behavior(e -> {
                    e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(), null);
                }).transition("a").transition("b").endActivity()
                .createActivity("a").behavior(new WaitState());
        if (exits) builder.transition("after");
        builder.endActivity();
        for (int i = 0; i < depth; i++) builder.createActivity("nested" + i).scope();
        builder.createActivity("b").scope();
        if (subprocess) {
            builder.behavior(new SubProcessActivityBehavior() {
                @Override public void execute(ActivityExecution e) { e.createSubProcessInstance(childDefinition).start(); }
                @Override public void completing(DelegateExecution e, DelegateExecution child) { callbacks.add((ActivityExecution)e); }
                @Override public void completed(ActivityExecution e) { callbacks.add(e); e.end(); }
            });
        } else builder.behavior(new WaitState());
        builder.endActivity();
        for (int i = 0; i < depth; i++) builder.endActivity();
        builder.endActivity().createActivity("after").behavior(new WaitState()).endActivity();
        ExecutionImpl root = (ExecutionImpl) builder.buildProcessDefinition().createProcessInstance();
        root.setVariable("shared", "root");
        root.start();
        ExecutionImpl waiting = root.findExecution("b");
        ExecutionImpl childProcess = waiting.getSubProcessInstance();
        ExecutionImpl top = waiting;
        for (int i = 0; i < depth; i++) top = top.getParent();
        ExecutionImpl carrier = top.getParent();
        carrier.setVariablesLocal(Collections.singletonMap("carrier", "retained"));
        waiting.setVariablesLocal(Collections.singletonMap("shared", "leaf"));
        if (childFirst) {
            waiting.signal(null, null);
            assertEquals(Collections.singletonList("a"), root.findActiveActivityIds());
            assertFalse(root.isEnded());
            root.findExecution("a").signal(null, null);
        } else {
            root.findExecution("a").signal(null, null);
            assertSame(waiting, root.findExecution("b"));
            ExecutionImpl replacement = carrier.getReplacedBy();
            assertNotNull(replacement);
            assertSame(replacement, top.getParent());
            assertTrue(replacement.getExecutions().contains(top));
            assertFalse(replacement.getExecutions().contains(carrier));
            assertTrue(carrier.getExecutions().isEmpty());
            assertEquals("retained", replacement.getVariable("carrier"));
            assertEquals("leaf", waiting.getVariable("shared"));
            assertEquals("root", root.getVariable("shared"));
            ExecutionImpl node = top;
            for (int i = 0; i < depth; i++) {
                assertEquals(1, node.getExecutions().size());
                ExecutionImpl next = node.getExecutions().get(0);
                assertSame(node, next.getParent());
                node = next;
            }
            assertSame(waiting, node);
            List<String> active = root.findActiveActivityIds();
            Collections.sort(active);
            assertEquals(exits ? Arrays.asList("after", "b") : Collections.singletonList("b"), active);
            if (afterFirst) {
                root.findExecution("after").signal(null, null);
                assertFalse(root.isEnded());
                assertSame(waiting, root.findExecution("b"));
            }
            if (subprocess) {
                assertSame(childProcess, waiting.getSubProcessInstance());
                assertSame(waiting, childProcess.getSuperExecution());
                childProcess.findExecution("childWait").signal(null, null);
                assertEquals(Arrays.asList(waiting, waiting), callbacks);
                assertTrue(childProcess.isEnded());
                assertNull(waiting.getSubProcessInstance());
            } else waiting.signal(null, null);
        }
        if (exits && !afterFirst) {
            assertFalse(root.isEnded());
            assertEquals(Collections.singletonList("after"), root.findActiveActivityIds());
            root.findExecution("after").signal(null, null);
        }
        assertTrue(root.isEnded());
        assertTrue(root.getExecutions().isEmpty());
        assertTrue(root.findActiveActivityIds().isEmpty());
    }
}
