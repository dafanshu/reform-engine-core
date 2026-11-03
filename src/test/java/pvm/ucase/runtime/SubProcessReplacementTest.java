package pvm.ucase.runtime;

import com.cloud.engine.delegate.DelegateExecution;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.delegate.SubProcessActivityBehavior;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.WaitState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubProcessReplacementTest extends TestCase {
    public void testWaitingBranchEndsBeforeSubProcess() { verify(false, false); }
    public void testSubProcessEndsBeforeWaitingBranch() { verify(true, false); }
    public void testScopeExitTransfersRemainingSubProcess() { verify(false, true); }

    private void verify(boolean childFirst, boolean scopedFork) {
        PvmProcessDefinition childDefinition = new ProcessDefinitionBuilder("child")
                .createActivity("childWait").initial().behavior(new WaitState()).endActivity()
                .buildProcessDefinition();
        List<ActivityExecution> callbacks = new ArrayList<ActivityExecution>();
        SubProcessActivityBehavior call = new SubProcessActivityBehavior() {
            @Override public void execute(ActivityExecution execution) {
                execution.createSubProcessInstance(childDefinition).start();
            }
            @Override public void completing(DelegateExecution execution, DelegateExecution child) {
                callbacks.add((ActivityExecution) execution);
            }
            @Override public void completed(ActivityExecution execution) {
                callbacks.add(execution);
                execution.end();
            }
        };
        ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("parent");
        if (scopedFork) builder.createActivity("outer").scope();
        builder.createActivity("fork").initial()
                .behavior(e -> e.takeAll(e.getActivity().getOutgoingTransitions(), null))
                .transition("a").transition("call").endActivity()
                .createActivity("a").behavior(new WaitState());
        if (scopedFork) builder.transition("after");
        builder.endActivity().createActivity("call").behavior(call).endActivity();
        if (scopedFork) builder.endActivity().createActivity("after").behavior(new WaitState()).endActivity();
        ExecutionImpl root = (ExecutionImpl) builder.buildProcessDefinition().createProcessInstance();
        root.start();
        ExecutionImpl originalCaller = root.findExecution("call");
        ExecutionImpl child = originalCaller.getSubProcessInstance();
        ExecutionImpl a = root.findExecution("a");
        if (childFirst) {
            child.findExecution("childWait").signal(null, null);
            assertEquals(2, callbacks.size());
            assertSame(originalCaller, callbacks.get(0));
            assertSame(originalCaller, callbacks.get(1));
            root.findExecution("a").signal(null, null);
        } else {
            a.signal(null, null);
            ExecutionImpl replacement = root.findExecution("call");
            assertNotSame(originalCaller, replacement);
            assertSame(replacement, originalCaller.getReplacedBy());
            assertSame(child, replacement.getSubProcessInstance());
            assertSame(replacement, child.getSuperExecution());
            assertNull(originalCaller.getSubProcessInstance());
            child.findExecution("childWait").signal(null, null);
            assertEquals(2, callbacks.size());
            assertSame(replacement, callbacks.get(0));
            assertSame(replacement, callbacks.get(1));
            assertNull(replacement.getSubProcessInstance());
            if (scopedFork) {
                assertEquals(Collections.singletonList("after"), root.findActiveActivityIds());
                root.findExecution("after").signal(null, null);
            }
        }
        assertTrue(child.isEnded());
        assertTrue(root.isEnded());
        assertTrue(root.findActiveActivityIds().isEmpty());
    }
}
