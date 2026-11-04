package pvm.ucase.runtime;

import com.cloud.engine.ActivitiException;
import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.io.IOException;
import java.util.*;

public class FailurePropagationContractTest extends TestCase {
    private List<Exception> failures() {
        return Arrays.asList(new IllegalStateException("runtime failure"), new IOException("checked failure"));
    }
    private void assertReported(Exception failure, Runnable operation) {
        assertReported(failure, PvmException.class, operation);
    }
    private void assertReported(Exception failure, Class<? extends RuntimeException> wrapper, Runnable operation) {
        try { operation.run(); fail("the operation must report failure to its caller"); }
        catch (RuntimeException actual) {
            if (failure instanceof RuntimeException) assertSame(failure, actual);
            else {assertEquals(wrapper, actual.getClass()); assertSame(failure, actual.getCause());}
        }
    }

    public void testParallelNodeFailureStopsRemainingDispatchWithoutRollback() {
        for (Exception failure : failures()) {
            List<String> effects = new ArrayList<String>();
            ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("parallel")
                    .executionListener(PvmEvent.EVENTNAME_END, e -> effects.add("process-end"))
                    .createActivity("fork").initial().behavior(e -> {
                        e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(), null);
                    }).transition("service").transition("later").endActivity()
                    .createActivity("service").behavior(e -> {
                        e.setVariable("written", true); effects.add("service"); throw failure;
                    }).endActivity()
                    .createActivity("later").behavior(e -> effects.add("later")).endActivity()
                    .buildProcessDefinition().createProcessInstance();
            assertReported(failure, root::start);
            assertEquals(Collections.singletonList("service"), effects);
            assertEquals(true, root.getVariable("written")); assertFalse(root.isEnded());
            assertNotNull(root.findExecution("service"));
            // takeAll prepares both carriers before dispatch; a thrown callback does not roll them back.
            assertEquals(2, root.getExecutions().size());
        }
    }

    public void testSignalFailureReachesSignalCaller() {
        for (Exception failure : failures()) {
            List<String> effects = new ArrayList<String>();
            ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("signal")
                    .executionListener(PvmEvent.EVENTNAME_END, e -> effects.add("process-end"))
                    .createActivity("wait").initial().behavior(new SignallableActivityBehavior() {
                        public void execute(ActivityExecution e) { }
                        public void signal(ActivityExecution e, String name, Object data) throws Exception {
                            e.setVariable("written", data); effects.add("signal"); throw failure;
                        }
                    }).endActivity().buildProcessDefinition().createProcessInstance();
            root.start(); assertReported(failure, () -> root.signal("approve", "kept"));
            assertEquals(Collections.singletonList("signal"), effects);
            assertEquals("kept", root.getVariable("written")); assertFalse(root.isEnded());
            assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
        }
    }

    public void testStartListenerFailureDoesNotExecuteActivity() { listener(false); }
    public void testEndListenerFailureIsReportedEvenThoughRootIsEnded() { listener(true); }
    private void listener(boolean atEnd) {
        for (Exception failure : failures()) {
            List<String> effects = new ArrayList<String>();
            String event = atEnd ? PvmEvent.EVENTNAME_END : PvmEvent.EVENTNAME_START;
            ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("listener")
                    .executionListener(event, e -> effects.add("first"))
                    .executionListener(event, e -> { e.setVariable("written", true); effects.add("failed"); throw failure; })
                    .executionListener(event, e -> effects.add("later"))
                    .createActivity("work").initial().behavior(e -> {effects.add("work"); e.end();}).endActivity()
                    .buildProcessDefinition().createProcessInstance();
            assertReported(failure, root::start);
            assertEquals(atEnd ? Arrays.asList("work", "first", "failed") : Arrays.asList("first", "failed"), effects);
            assertEquals(true, root.getVariable("written")); assertEquals(atEnd, root.isEnded());
            // ended denotes control-flow termination, not successful completion of all callbacks.
        }
    }

    public void testCompletingFailureRetainsChildRelationAndSkipsCompleted() { subprocess(false); }
    public void testCompletedFailureIsReportedAfterChildRelationIsCleared() { subprocess(true); }
    private void subprocess(boolean inCompleted) {
        for (Exception failure : failures()) {
            List<String> effects = new ArrayList<String>();
            PvmProcessDefinition childDefinition = new ProcessDefinitionBuilder("child")
                    .executionListener(PvmEvent.EVENTNAME_END, e -> effects.add("child-end"))
                    .createActivity("scope").scope()
                        .createActivity("childWait").initial().behavior(new WaitState()).endActivity()
                    .endActivity().buildProcessDefinition();
            ExecutionImpl root = (ExecutionImpl)new ProcessDefinitionBuilder("parent")
                    .executionListener(PvmEvent.EVENTNAME_END, e -> effects.add("parent-end"))
                    .createActivity("call").initial().behavior(new SubProcessActivityBehavior() {
                        public void execute(ActivityExecution e) {e.createSubProcessInstance(childDefinition).start();}
                        public void completing(DelegateExecution e, DelegateExecution child) throws Exception {
                            effects.add("completing"); e.setVariable("partialResult", "kept");
                            if (!inCompleted) throw failure;
                        }
                        public void completed(ActivityExecution e) throws Exception {
                            effects.add("completed"); throw failure;
                        }
                    }).endActivity().buildProcessDefinition().createProcessInstance();
            root.start(); ExecutionImpl child = root.getSubProcessInstance();
            assertReported(failure, ActivitiException.class, () -> child.findExecution("childWait").signal(null, null));
            assertEquals(inCompleted ? Arrays.asList("child-end", "completing", "completed") : Arrays.asList("child-end", "completing"), effects);
            assertEquals("kept", root.getVariable("partialResult"));
            assertTrue(child.isEnded()); assertFalse(root.isEnded());
            if (inCompleted) assertNull(root.getSubProcessInstance()); else assertSame(child, root.getSubProcessInstance());
        }
    }
}
