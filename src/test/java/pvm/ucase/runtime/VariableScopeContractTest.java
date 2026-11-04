package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.*;

public class VariableScopeContractTest extends TestCase {
    public void testInstancesAndValueReferences() {
        PvmProcessDefinition d = definition();
        ExecutionImpl a = (ExecutionImpl) d.createProcessInstance(), b = (ExecutionImpl) d.createProcessInstance();
        a.setVariable("amount", 1);
        assertFalse(b.hasVariable("amount"));
        List<String> shared = new ArrayList<String>();
        a.setVariable("list", shared); b.setVariable("list", shared);
        shared.add("shared by caller");
        assertSame(a.getVariable("list"), b.getVariable("list"));
        Map<String, Object> copy = a.getVariables(); copy.clear();
        assertTrue(a.hasVariable("list"));
        a.start(); a.signal(null, null);
        assertTrue(a.isEnded()); assertEquals(1, a.getVariable("amount"));
        assertFalse(b.isEnded());
    }

    public void testNearestBindingAndNewVariableAtRoot() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("name", "root");
        ExecutionImpl child = root.createExecution(), leaf = child.createExecution();
        child.setVariablesLocal(Collections.singletonMap("name", "child"));
        leaf.setVariable("name", "changed"); leaf.setVariable("new", 1);
        assertEquals("root", root.getVariable("name"));
        assertEquals("changed", child.getVariable("name"));
        assertEquals(1, root.getVariablesLocal().get("new"));
        assertTrue(leaf.getVariablesLocal().isEmpty());
    }

    public void testFreshLocalWriterInitializesContainer() {
        ExecutionImpl e = new ExecutionImpl(); e.setVariableLocally("key", 1);
        assertEquals(1, e.getVariable("key"));
    }

    public void testLocalSingleValueAndTypedOverloads() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("value", "root");
        ExecutionImpl child = root.createExecution();
        child.setVariableLocal("value", "child", false);
        assertEquals("child", child.getVariableLocal("value"));
        assertEquals("child", child.getVariableLocal("value", true));
        assertEquals("child", child.getVariableLocal("value", String.class));
        assertEquals("root", root.getVariable("value"));
        assertNull(root.getVariableLocal("missing"));
    }

    public void testNullShadowAndPresence() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("key", "root");
        ExecutionImpl child = root.createExecution();
        assertTrue(child.hasVariables()); assertFalse(child.hasVariablesLocal());
        child.setVariablesLocal(Collections.singletonMap("key", null));
        assertNull(child.getVariable("key")); assertTrue(child.hasVariableLocal("key"));
        assertTrue(child.hasVariablesLocal()); assertFalse(child.hasVariableLocal("missing"));
    }

    public void testNamesAndFilteredLocalSnapshots() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("inherited", 1);
        ExecutionImpl child = root.createExecution(); child.setVariablesLocal(Collections.singletonMap("local", 2));
        assertEquals(new HashSet<String>(Arrays.asList("inherited", "local")), child.getVariableNames());
        Set<String> names = child.getVariableNamesLocal(); names.clear();
        assertEquals(Collections.singleton("local"), child.getVariableNamesLocal());
        Map<String,Object> selected = child.getVariablesLocal(Arrays.asList("local", "inherited", "missing"), false);
        assertEquals(2, selected.get("local"));
        assertTrue(selected.containsKey("missing")); assertNull(selected.get("inherited"));
        selected.clear(); assertEquals(2, child.getVariablesLocal().get("local"));
    }

    public void testRemoveNearestBindingRevealsParent() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("key", "root");
        ExecutionImpl child = root.createExecution(); child.setVariablesLocal(Collections.singletonMap("key", null));
        ExecutionImpl leaf = child.createExecution(); leaf.removeVariable("key");
        assertFalse(child.getVariablesLocal().containsKey("key")); assertEquals("root", leaf.getVariable("key"));
        leaf.removeVariableLocal("key"); assertTrue(root.hasVariable("key"));
        leaf.removeVariable("key"); assertFalse(root.hasVariable("key"));
        leaf.removeVariable("missing");
    }

    public void testBulkDeletionUsesVisibleNamesSnapshot() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("same", "root"); root.setVariable("rootOnly", 1);
        ExecutionImpl child = root.createExecution();
        child.setVariablesLocal(Collections.singletonMap("same", "child")); child.setVariablesLocal(Collections.singletonMap("local", 2));
        child.removeVariables();
        assertTrue(child.getVariablesLocal().isEmpty());
        assertEquals(Collections.singletonMap("same", "root"), root.getVariables());
        child.setVariablesLocal(Collections.singletonMap("local", 3));
        child.removeVariablesLocal(Arrays.asList("same", "local"));
        assertEquals("root", root.getVariable("same"));
        child.setVariablesLocal(Collections.singletonMap("local", 4)); child.deleteVariablesLocal();
        assertTrue(child.getVariablesLocal().isEmpty());
        child.removeVariables(Arrays.asList("same", "missing")); assertTrue(root.getVariables().isEmpty());
    }

    public void testLocalClearPreservesAncestors() {
        ExecutionImpl root = new ExecutionImpl(); root.setVariable("key", 1);
        ExecutionImpl child = root.createExecution(); child.setVariablesLocal(Collections.singletonMap("key", 2));
        child.removeVariablesLocal(); assertEquals(1, child.getVariable("key"));
    }

    public void testLazyVariableAndParentHooksRemainEffective() {
        final ExecutionImpl root = new ExecutionImpl(); root.setVariable("inherited", 1);
        ExecutionImpl child = new ExecutionImpl() {
            @Override protected void ensureVariablesInitialized() {
                if (variables == null) {
                    variables = new HashMap<String, Object>();
                    variables.put("loaded", 2);
                }
            }
            @Override protected void ensureParentInitialized() {
                if (parent == null) parent = root;
            }
        };
        assertEquals(2, child.getVariableLocal("loaded"));
        child.setVariableLocally("written", 3);
        assertEquals(2, child.getVariableLocal("loaded"));
        assertEquals(3, child.getVariableLocal("written"));
        child.removeVariable("inherited");
        assertFalse(root.hasVariable("inherited"));
        child.removeVariablesLocal(); assertTrue(child.getVariablesLocal().isEmpty());
    }

    public void testFailurePreservesWrittenVariables() {
        PvmProcessDefinition d = new ProcessDefinitionBuilder("failure")
                .createActivity("work").initial().behavior(e -> {e.setVariable("written", 1); throw new IllegalStateException("failed");})
                .endActivity().buildProcessDefinition();
        ExecutionImpl root = (ExecutionImpl) d.createProcessInstance();
        try {root.start(); fail();} catch (IllegalStateException expected) {assertEquals("failed", expected.getMessage());}
        assertEquals(1, root.getVariable("written")); assertFalse(root.isEnded());
    }

    public void testFirstBranchEndsBeforeSecond() { pruning(true); }
    public void testSecondBranchEndsBeforeFirst() { pruning(false); }
    private void pruning(boolean aFirst) {
        ExecutionImpl root = (ExecutionImpl) new ProcessDefinitionBuilder("pruning")
                .createActivity("fork").initial().behavior(e -> {e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(), null);})
                    .transition("a").transition("b").endActivity()
                .createActivity("a").behavior(new WaitState()).endActivity()
                .createActivity("b").behavior(new WaitState()).endActivity().buildProcessDefinition().createProcessInstance();
        root.setVariable("same", "parent"); root.start();
        root.findExecution("a").setVariablesLocal(Collections.singletonMap("same", "a"));
        root.findExecution("b").setVariablesLocal(Collections.singletonMap("same", "b"));
        String first = aFirst ? "a" : "b", survivor = aFirst ? "b" : "a";
        root.findExecution(first).signal(null, null);
        assertEquals(survivor, root.getVariable("same")); // Existing survivor-to-parent merge, not an order-independent reducer.
        root.findExecution(survivor).signal(null, null);
        assertTrue(root.isEnded()); assertEquals(survivor, root.getVariable("same"));
    }

    public void testSubprocessRequiresExplicitInputAndOutput() {
        PvmProcessDefinition childDefinition = definition();
        List<ExecutionImpl> children = new ArrayList<ExecutionImpl>();
        PvmProcessDefinition parentDefinition = new ProcessDefinitionBuilder("parent")
                .createActivity("call").initial().behavior(new SubProcessActivityBehavior() {
                    public void execute(ActivityExecution e) {
                        ExecutionImpl child = (ExecutionImpl) e.createSubProcessInstance(childDefinition);
                        assertNull(child.getVariable("input"));
                        child.setVariable("input", e.getVariable("input"));
                        child.setVariable("result", "child-only"); children.add(child); child.start();
                    }
                    public void completing(DelegateExecution e, DelegateExecution child) {e.setVariable("result", child.getVariable("result"));}
                    public void completed(ActivityExecution e) {e.end();}
                }).endActivity().buildProcessDefinition();
        ExecutionImpl root = (ExecutionImpl) parentDefinition.createProcessInstance(); root.setVariable("input", "explicit"); root.start();
        assertNull(root.getVariable("result"));
        ExecutionImpl child = children.get(0); assertEquals("explicit", child.getVariable("input"));
        child.signal(null, null);
        assertTrue(root.isEnded()); assertTrue(child.isEnded());
        assertEquals("child-only", root.getVariable("result")); assertEquals("child-only", child.getVariable("result"));
    }

    private PvmProcessDefinition definition() {
        return new ProcessDefinitionBuilder("wait").createActivity("wait").initial().behavior(new WaitState()).endActivity().buildProcessDefinition();
    }
}
