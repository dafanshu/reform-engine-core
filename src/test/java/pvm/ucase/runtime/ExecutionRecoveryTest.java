package pvm.ucase.runtime;

import com.cloud.engine.delegate.ExecutionListener;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmException;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.runtime.AtomicOperation;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import com.cloud.engine.pvm.runtime.InterpretableExecution;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionRecoveryTest extends TestCase {

    public void testActivityCanRecoverAfterConsecutiveRuntimeFailures() {
        assertActivityCanRecover(new IllegalStateException("temporary failure"), 2);
    }

    public void testActivityCanRecoverAfterCheckedException() {
        assertActivityCanRecover(new IOException("temporary failure"), 1);
    }

    private void assertActivityCanRecover(Exception failure, int failures) {
        AtomicInteger attempts = new AtomicInteger();
        PvmProcessDefinition definition = new ProcessDefinitionBuilder("recovery")
                .createActivity("work")
                    .initial()
                    .behavior(execution -> {
                        if (attempts.incrementAndGet() <= failures) {
                            throw failure;
                        }
                        execution.end();
                    })
                .endActivity()
                .buildProcessDefinition();
        ExecutionImpl process = (ExecutionImpl) definition.createProcessInstance();

        for (int attempt = 1; attempt <= failures; attempt++) {
            try {
                if (attempt == 1) {
                    process.start();
                } else {
                    process.executeActivity(definition.findActivity("work"));
                }
                fail("The activity failure must reach the caller");
            } catch (RuntimeException actual) {
                if (failure instanceof RuntimeException) {
                    assertSame(failure, actual);
                } else {
                    assertTrue(actual instanceof PvmException);
                    assertSame(failure, actual.getCause());
                }
            }
            assertEquals("No automatic retry is allowed", attempt, attempts.get());
            assertFalse(process.isEnded());
            assertEquals(Collections.singletonList("work"), process.findActiveActivityIds());
        }

        process.executeActivity(definition.findActivity("work"));

        assertEquals(failures + 1, attempts.get());
        assertTrue(process.isEnded());
        assertTrue(process.findActiveActivityIds().isEmpty());
    }

    public void testProcessStartListenerCanResumeWithoutRepeatingCompletedListeners() {
        List<String> events = new ArrayList<>();
        AtomicInteger secondListenerAttempts = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("listener failure");
        PvmProcessDefinition definition = new ProcessDefinitionBuilder("listener-recovery")
                .executionListener(ExecutionListener.EVENTNAME_START,
                        execution -> events.add("first"))
                .executionListener(ExecutionListener.EVENTNAME_START, execution -> {
                    events.add("second");
                    if (secondListenerAttempts.incrementAndGet() == 1) {
                        throw failure;
                    }
                })
                .createActivity("work")
                    .initial()
                    .behavior(execution -> {
                        events.add("work");
                        execution.end();
                    })
                .endActivity()
                .buildProcessDefinition();
        ExecutionImpl process = (ExecutionImpl) definition.createProcessInstance();

        try {
            process.start();
            fail("The listener failure must reach the caller");
        } catch (IllegalStateException actual) {
            assertSame(failure, actual);
        }
        assertEquals(Arrays.asList("first", "second"), events);
        assertFalse(process.isEnded());

        // Retry the same process-start operation, preserving the listener checkpoint.
        process.start();

        assertEquals(Arrays.asList("first", "second", "second", "work"), events);
        assertTrue(process.isEnded());
        assertTrue(process.findActiveActivityIds().isEmpty());
        assertNull(process.getEventName());
        assertNull(process.getEventSource());
        assertEquals(Integer.valueOf(0), process.getExecutionListenerIndex());
    }

    public void testErrorDiscardsPendingOperationAndAllowsExplicitRecovery() {
        InspectableExecution execution = new InspectableExecution();
        List<String> events = new ArrayList<>();
        AssertionError failure = new AssertionError("operation failure");
        AtomicOperation pending = new SynchronousOperation() {
            @Override
            public void execute(InterpretableExecution current) {
                events.add("pending");
            }
        };

        try {
            execution.performOperation(new SynchronousOperation() {
                @Override
                public void execute(InterpretableExecution current) {
                    events.add("failed");
                    current.setVariable("writtenBeforeFailure", "retained");
                    current.performOperation(pending);
                    throw failure;
                }
            });
            fail("The Error must reach the caller");
        } catch (AssertionError actual) {
            assertSame(failure, actual);
        }

        assertEquals(Collections.singletonList("failed"), events);
        assertFalse("Failed dispatch must discard queued work", execution.hasPendingOperation());
        assertEquals("Recovery does not roll back business state",
                "retained", execution.getVariable("writtenBeforeFailure"));

        execution.performOperation(new SynchronousOperation() {
            @Override
            public void execute(InterpretableExecution current) {
                events.add("recovered");
            }
        });

        assertEquals(Arrays.asList("failed", "recovered"), events);
        assertFalse(execution.hasPendingOperation());
    }

    private static class InspectableExecution extends ExecutionImpl {
        boolean hasPendingOperation() {
            return nextOperation != null;
        }
    }

    private abstract static class SynchronousOperation implements AtomicOperation {
        @Override
        public boolean isAsync(InterpretableExecution execution) {
            return false;
        }
    }
}
