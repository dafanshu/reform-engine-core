package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.Collections;

public class ExecutionStateRegressionTest extends TestCase {
    public void testStartInitializesLazyProcessDefinition() {
        final ProcessDefinitionImpl definition = (ProcessDefinitionImpl) new ProcessDefinitionBuilder("lazy")
                .createActivity("wait").initial().behavior(new WaitState()).endActivity()
                .buildProcessDefinition();
        ExecutionImpl execution = new ExecutionImpl() {
            @Override protected void ensureProcessDefinitionInitialized() {
                if (processDefinition == null) setProcessDefinition(definition);
            }
        };
        execution.setProcessInstance(execution);
        execution.start();
        assertSame(definition, execution.getProcessDefinition());
        assertEquals(Collections.singletonList("wait"), execution.findActiveActivityIds());
        assertNull(execution.getStartingExecution());
    }

    public void testStartPreservesExplicitInitialActivity() {
        ProcessDefinitionImpl definition = (ProcessDefinitionImpl) new ProcessDefinitionBuilder("explicit")
                .createActivity("default").initial().behavior(new WaitState()).endActivity()
                .createActivity("chosen").behavior(new WaitState()).endActivity().buildProcessDefinition();
        ExecutionImpl execution = (ExecutionImpl) definition.createProcessInstanceForInitial(definition.findActivity("chosen"));
        execution.start();
        assertEquals(Collections.singletonList("chosen"), execution.findActiveActivityIds());
    }

    public void testInactiveMatchRemainsFindableButIsNotActive() {
        ExecutionImpl execution = tree();
        assertFalse(execution.isActive("missing"));
        assertTrue(execution.isActive("wait"));
        execution.inactivate();
        assertSame(execution, execution.findExecution("wait"));
        assertFalse(execution.isActive("wait"));
    }

    public void testInactiveParentDoesNotHideActiveDescendantWithSameId() {
        ExecutionImpl root = tree();
        root.inactivate();
        ExecutionImpl child = root.createExecution();
        child.inactivate();
        ExecutionImpl grandchild = child.createExecution();
        assertSame(root, root.findExecution("wait"));
        assertTrue(root.isActive("wait"));
        grandchild.inactivate();
        assertFalse(root.isActive("wait"));
    }

    public void testInactiveSiblingDoesNotHideLaterActiveMatch() {
        ExecutionImpl root = tree();
        ActivityImpl wait = root.getActivity();
        root.setActivity(null);
        ExecutionImpl first = root.createExecution();
        first.setActivity(wait);
        first.inactivate();
        ExecutionImpl second = root.createExecution();
        second.setActivity(wait);
        assertSame(first, root.findExecution("wait"));
        assertTrue(root.isActive("wait"));
        second.inactivate();
        assertFalse(root.isActive("wait"));
    }

    private ExecutionImpl tree() {
        ProcessDefinitionImpl definition = new ProcessDefinitionImpl("tree");
        ExecutionImpl execution = new ExecutionImpl();
        execution.setProcessDefinition(definition);
        execution.setProcessInstance(execution);
        execution.setActivity(definition.createActivity("wait"));
        return execution;
    }
}
