package pvm.ucase.runtime;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class LocalVariablePruningTest extends TestCase {
    public void testActivityEndPreservesLocalBindings() { verify(false); }
    public void testScopeExitPreservesLocalBindings() { verify(true); }
    private void verify(boolean scopeExit) {
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("p");
        if (scopeExit) builder.createActivity("outer").scope();
        builder.createActivity("fork").initial().behavior(e -> {
            e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(), null);
        }).transition("a").transition("b").endActivity()
                .createActivity("a").behavior(new WaitState());
        if (scopeExit) builder.transition("after");
        builder.endActivity().createActivity("b").behavior(new WaitState()).endActivity();
        if (scopeExit) builder.endActivity().createActivity("after").behavior(new WaitState()).endActivity();
        ExecutionImpl root = (ExecutionImpl) builder.buildProcessDefinition().createProcessInstance();
        root.setVariable("shared", "parent");
        root.setVariable("retained", "root-only");
        root.start();
        ExecutionImpl a = root.findExecution("a"), b = root.findExecution("b");
        b.getVariables();
        b.setVariableLocally("shared", "branch");
        b.setVariableLocally("approval", "approved");
        b.setVariableLocally("nullable", null);
        assertEquals("branch", b.getVariable("shared"));
        assertEquals("parent", a.getVariable("shared"));
        assertNull(a.getVariable("approval"));
        a.signal(null, null);
        ExecutionImpl survivor = root.findExecution("b");
        assertNotSame(b, survivor);
        assertEquals("approved", survivor.getVariable("approval"));
        assertEquals("branch", survivor.getVariable("shared"));
        assertEquals("root-only", survivor.getVariable("retained"));
        assertTrue(survivor.getVariablesLocal().containsKey("nullable"));
        if (scopeExit) {
            assertEquals("parent", root.getVariable("shared"));
            assertEquals("parent", root.findExecution("after").getVariable("shared"));
            assertNull(root.findExecution("after").getVariable("approval"));
            assertFalse(survivor.getVariablesLocal().containsKey("retained"));
        }
        b.setVariableLocally("approval", "detached-change");
        assertEquals("approved", survivor.getVariable("approval"));
    }
    public void testLocalMapSnapshotAndMerge() {
        ExecutionImpl root = new ExecutionImpl();
        root.setVariable("inherited", "root");
        ExecutionImpl child = root.createExecution();
        child.setVariablesLocal(Collections.singletonMap("shared", "child"));
        assertEquals("child", child.getVariable("shared"));
        assertNull(root.getVariable("shared"));
        Map<String,Object> snapshot = child.getVariablesLocal();
        assertFalse(snapshot.containsKey("inherited"));
        snapshot.put("shared", "external-change");
        assertEquals("child", child.getVariable("shared"));
        child.setVariablesLocal(Collections.singletonMap("other", "new"));
        child.setVariablesLocal(null);
        assertEquals("child", child.getVariable("shared"));
        assertEquals("new", child.getVariable("other"));
    }
}
