package pvm.ucase.runtime;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class ParallelScopeCompletionTest extends TestCase {
    public void testScopedBranchFirst() { verify(true, false); }
    public void testOrdinaryBranchFirst() { verify(false, false); }
    public void testNestedScopedBranchFirst() { verify(true, true); }
    public void testNestedOrdinaryBranchFirst() { verify(false, true); }
    private void verify(boolean scopedFirst, boolean nested) {
        List<String> events = new ArrayList<String>();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("p")
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("process"));
        if (nested) builder.createActivity("outer").scope()
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("outer"));
        builder.createActivity("fork").initial().behavior(e -> {
            e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(), null);
        }).transition("a").transition("b").endActivity()
                .createActivity("a").scope().behavior(new WaitState())
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("a")).endActivity()
                .createActivity("b").behavior(new WaitState())
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("b")).endActivity();
        if (nested) builder.endActivity();
        ExecutionImpl root = (ExecutionImpl) builder.buildProcessDefinition().createProcessInstance();
        root.start();
        String first = scopedFirst ? "a" : "b", second = scopedFirst ? "b" : "a";
        root.findExecution(first).signal(null, null);
        assertFalse(root.isEnded());
        assertEquals(Collections.singletonList(second), root.findActiveActivityIds());
        assertEquals(Collections.singletonList(first), events);
        root.findExecution(second).signal(null, null);
        assertTrue(root.isEnded());
        assertTrue(root.findActiveActivityIds().isEmpty());
        assertTrue(root.getExecutions().isEmpty());
        List<String> expected = new ArrayList<String>(Arrays.asList(first, second));
        if (nested) expected.add("outer");
        expected.add("process");
        assertEquals(expected, events);
    }
}
