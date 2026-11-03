package pvm.ucase.runtime;

import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;

public class LazyExecutionRelationIdTest extends TestCase {
    public void testParentIdLoadsRelationOnFirstCall() {
        ExecutionImpl loadedParent = new ExecutionImpl();
        loadedParent.setId("parent-id");
        ExecutionImpl child = new ExecutionImpl() {
            @Override protected void ensureParentInitialized() { setParent(loadedParent); }
        };
        assertEquals("parent-id", child.getParentId());
        assertSame(loadedParent, child.getParent());
    }

    public void testSuperExecutionIdLoadsRelationOnFirstCall() {
        ExecutionImpl caller = new ExecutionImpl();
        caller.setId("caller-id");
        ExecutionImpl child = new ExecutionImpl() {
            @Override protected void ensureSuperExecutionInitialized() { superExecution = caller; }
        };
        assertEquals("caller-id", child.getSuperExecutionId());
        assertSame(caller, child.getSuperExecution());
    }

    public void testAbsentLazyRelationsReturnNull() {
        int[] loads = new int[2];
        ExecutionImpl execution = new ExecutionImpl() {
            @Override protected void ensureParentInitialized() { loads[0]++; }
            @Override protected void ensureSuperExecutionInitialized() { loads[1]++; }
            @Override protected void ensureActivityInitialized() { fail("Relation IDs must not load the activity"); }
        };
        assertNull(execution.getParentId());
        assertNull(execution.getSuperExecutionId());
        assertEquals(1, loads[0]);
        assertEquals(1, loads[1]);
    }
}
