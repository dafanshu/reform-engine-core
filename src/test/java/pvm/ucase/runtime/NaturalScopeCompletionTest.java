package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NaturalScopeCompletionTest extends TestCase {
    public void testSingleScopeNaturalCompletion() { verify(1, false); }
    public void testDeepScopeNaturalCompletion() { verify(3, false); }
    public void testScopedLeafNaturalCompletion() { verify(2, true); }
    public void testUnscopedControlCompletion() { verify(0, false); }

    private void verify(int depth, boolean scopedLeaf) {
        List<String> events = new ArrayList<String>();
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("natural")
                .executionListener(PvmEvent.EVENTNAME_END, e -> {
                    ActivityExecution execution = (ActivityExecution) e;
                    assertTrue("Process end listeners must see ended=true", execution.isEnded());
                    assertFalse(execution.isActive());
                    events.add("process");
                });
        for (int i = 0; i < depth; i++) {
            final String name = "scope" + i;
            builder.createActivity(name).scope()
                    .executionListener(PvmEvent.EVENTNAME_END, e -> events.add(name));
        }
        builder.createActivity("wait").initial().behavior(new WaitState())
                .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("wait"));
        if (scopedLeaf) builder.scope();
        builder.endActivity();
        for (int i = 0; i < depth; i++) builder.endActivity();
        ExecutionImpl root = (ExecutionImpl) builder.buildProcessDefinition().createProcessInstance();
        root.start();
        assertFalse(root.isEnded());
        assertEquals(Collections.singletonList("wait"), root.findActiveActivityIds());
        root.findExecution("wait").signal(null, null);
        assertTrue(root.isEnded());
        assertFalse(root.isActive());
        assertFalse(root.isScope());
        assertTrue(root.getExecutions().isEmpty());
        assertTrue(root.findActiveActivityIds().isEmpty());
        List<String> expected = new ArrayList<String>();
        expected.add("wait");
        for (int i = depth - 1; i >= 0; i--) expected.add("scope" + i);
        expected.add("process");
        assertEquals(expected, events);
    }
}
