package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import pvm.ucase.behavior.ParallelGateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TakeAllUnrelatedBranchTest extends TestCase {
    public void testScopedBranchSurvivesSingleOutgoingGateway() { scopeBranch(false); }
    public void testCompletedScopedBranchAllowsSingleOutgoingGateway() { scopeBranch(true); }

    private void scopeBranch(boolean scopeFirst) {
        List<String> events = new ArrayList<String>();
        ExecutionImpl root = (ExecutionImpl) new ProcessDefinitionBuilder("scope")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("process"))
                .createActivity("start").initial().behavior(new WaitState()).transition("fork").endActivity()
                .createActivity("fork").behavior(new ParallelGateway())
                    .transition("scope").transition("b").endActivity()
                .createActivity("scope").scope().behavior(new WaitState())
                    .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("scope")).endActivity()
                .createActivity("b").behavior(new WaitState()).transition("gateway").endActivity()
                .createActivity("gateway").behavior(new ParallelGateway()).transition("after").endActivity()
                .createActivity("after").behavior(new WaitState()).endActivity()
                .buildProcessDefinition().createProcessInstance();
        root.start();
        root.findExecution("start").signal(null, null);
        ExecutionImpl scope = root.findExecution("scope");
        assertFalse("scope carrier is inactive", scope.getParent().isActive());
        if (scopeFirst) scope.signal(null, null);
        root.findExecution("b").signal(null, null);
        assertFalse(root.isEnded());
        root.findExecution("after").signal(null, null);
        if (!scopeFirst) {
            assertFalse("the scoped branch is still alive", root.isEnded());
            assertTrue(events.isEmpty());
            assertEquals(Collections.singletonList("scope"), root.findActiveActivityIds());
            assertSame(scope, root.findExecution("scope"));
            scope.signal(null, null);
        }
        assertCompleted(root);
        assertEquals(Arrays.asList("scope", "process"), events);
    }

    public void testTokenWaitingAtOtherJoinSurvivesSingleOutgoingGateway() { otherJoin(true); }
    public void testActiveSiblingSurvivesSingleOutgoingGateway() { otherJoin(false); }

    private void otherJoin(boolean waitingFirst) {
        List<String> events = new ArrayList<String>();
        ExecutionImpl root = (ExecutionImpl) new ProcessDefinitionBuilder("joins")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("process"))
                .createActivity("start").initial().behavior(new WaitState()).transition("fork").endActivity()
                .createActivity("fork").behavior(new ParallelGateway())
                    .transition("a").transition("b").endActivity()
                .createActivity("a").behavior(new WaitState()).transition("join").endActivity()
                .createActivity("b").behavior(new WaitState()).transition("gateway").endActivity()
                .createActivity("gateway").behavior(new ParallelGateway()).transition("join").endActivity()
                .createActivity("join").behavior(new ParallelGateway()).transition("after").endActivity()
                .createActivity("after").behavior(new WaitState()).endActivity()
                .buildProcessDefinition().createProcessInstance();
        root.start();
        root.findExecution("start").signal(null, null);
        String first = waitingFirst ? "a" : "b";
        String second = waitingFirst ? "b" : "a";
        root.findExecution(first).signal(null, null);
        assertFalse(root.findExecution("join").isActive());
        assertFalse(root.isEnded());
        assertTrue(events.isEmpty());
        root.findExecution(second).signal(null, null);
        assertEquals("both tokens must reach the join", Collections.singletonList("after"), root.findActiveActivityIds());
        assertSame("the completed join can collapse to its root", root, root.findExecution("after"));
        assertTrue(root.getExecutions().isEmpty());
        root.findExecution("after").signal(null, null);
        assertCompleted(root);
        assertEquals(Collections.singletonList("process"), events);
    }

    public void testSinglePathStillCollapsesToRoot() {
        ExecutionImpl root = (ExecutionImpl) new ProcessDefinitionBuilder("single")
                .createActivity("start").initial().behavior(new WaitState()).transition("gateway").endActivity()
                .createActivity("gateway").behavior(new ParallelGateway()).transition("after").endActivity()
                .createActivity("after").behavior(new WaitState()).endActivity()
                .buildProcessDefinition().createProcessInstance();
        root.start();
        root.signal(null, null);
        assertSame(root, root.findExecution("after"));
        assertTrue(root.getExecutions().isEmpty());
        root.signal(null, null);
        assertCompleted(root);
    }

    private void assertCompleted(ExecutionImpl root) {
        assertTrue(root.isEnded());
        assertFalse(root.isActive());
        assertTrue(root.getExecutions().isEmpty());
        assertTrue(root.findActiveActivityIds().isEmpty());
    }
}
