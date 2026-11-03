package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import org.junit.Test;
import pvm.WaitState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ConcurrentScopeEndNotificationTest {
  @Test public void ordinaryBranchAfterCrossing() { verify(false, 0); }
  @Test public void scopedBranchAfterCrossing() { verify(true, 0); }
  @Test public void ordinaryBranchBeforeCrossing() { verify(false, 1); }
  @Test public void scopedBranchBeforeCrossing() { verify(true, 1); }
  @Test public void ordinaryBranchAfterOutsideCompletion() { verify(false, 2); }
  @Test public void scopedBranchAfterOutsideCompletion() { verify(true, 2); }

  private void verify(boolean scopedB, int order) {
    List<String> events = new ArrayList<>();
    ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder("p")
        .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("process"))
        .createActivity("outer").scope()
          .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("outer"))
          .createActivity("fork").initial().behavior(e -> {
            e.inactivate();
            e.takeAll(e.getActivity().getOutgoingTransitions(), null);
          }).transition("a").transition("b").endActivity()
          .createActivity("a").behavior(new WaitState()).transition("after")
            .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("a")).endActivity()
          .createActivity("b").behavior(new WaitState())
            .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("b"));
    if (scopedB) builder.scope();
    ExecutionImpl root = (ExecutionImpl) builder.endActivity().endActivity()
        .createActivity("after").behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_END, e -> events.add("after")).endActivity()
        .buildProcessDefinition().createProcessInstance();
    root.start();
    assertEquals(Arrays.asList("a", "b"), root.findActiveActivityIds());
    assertTrue(events.isEmpty());
    if (order == 1) {
      root.findExecution("b").signal(null, null);
      assertEquals(Arrays.asList("b"), events);
      assertFalse(root.isEnded());
      root.findExecution("a").signal(null, null);
      assertEquals(Arrays.asList("b", "a", "outer"), events);
    } else {
      root.findExecution("a").signal(null, null);
      assertEquals(Arrays.asList("a", "outer"), events);
      assertFalse(root.isEnded());
      if (order == 2) {
        root.findExecution("after").signal(null, null);
        assertEquals(Arrays.asList("a", "outer", "after"), events);
        assertFalse(root.isEnded());
      }
      root.findExecution("b").signal(null, null);
      if (order == 2) {
        assertEquals(Arrays.asList("a", "outer", "after", "b", "outer", "process"), events);
      } else {
        assertEquals(Arrays.asList("a", "outer", "b", "outer"), events);
      }
    }
    if (order != 2) {
      assertFalse(root.isEnded());
      assertEquals(Arrays.asList("after"), root.findActiveActivityIds());
      root.findExecution("after").signal(null, null);
      assertEquals(order == 1
          ? Arrays.asList("b", "a", "outer", "after", "process")
          : Arrays.asList("a", "outer", "b", "outer", "after", "process"), events);
    }
    assertTrue(root.isEnded());
    assertFalse(root.isActive());
    assertTrue(root.findActiveActivityIds().isEmpty());
  }
}
