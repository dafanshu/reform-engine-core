package pvm.ucase.runtime;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.runtime.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.delegate.DelegateExecution;
import pvm.WaitState;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class TakeAllLifecycleContractTest {
  @Test public void CrossingFirstNoRecyclingInsideFirst() { gateway(false,false,false); }
  @Test public void CrossingFirstNoRecyclingOutsideFirst() { gateway(false,false,true); }
  @Test public void CrossingFirstRecycledInsideFirst() { gateway(false,true,false); }
  @Test public void CrossingFirstRecycledOutsideFirst() { gateway(false,true,true); }
  @Test public void GatewayFirstRecycledInsideFirst() { gateway(true,true,false); }
  @Test public void GatewayFirstRecycledOutsideFirst() { gateway(true,true,true); }
  private void gateway(boolean gatewayFirst, boolean recycle, boolean outsideFirst){
    List<String> events=new ArrayList<>();
    ExecutionImpl root=(ExecutionImpl)new ProcessDefinitionBuilder("p")
      .executionListener(PvmEvent.EVENTNAME_END,e->events.add("process"))
      .createActivity("outer").scope()
        .createActivity("fork").initial().behavior(e->{e.inactivate();e.takeAll(e.getActivity().getOutgoingTransitions(),null);})
          .transition("a").transition("b").endActivity()
        .createActivity("a").behavior(new WaitState()).transition("after").endActivity()
        .createActivity("b").behavior(new WaitState()).transition("gateway").endActivity()
        .createActivity("gateway").behavior(e->{ if(recycle) new pvm.ucase.behavior.ParallelGateway().execute(e); else {e.inactivate();e.takeAll(e.getActivity().getOutgoingTransitions(),null);} }).transition("c").endActivity()
        .createActivity("c").behavior(new WaitState()).endActivity()
      .endActivity().createActivity("after").behavior(new WaitState()).endActivity()
      .buildProcessDefinition().createProcessInstance();
    root.start();
    if(gatewayFirst)root.findExecution("b").signal(null,null);
    root.findExecution("a").signal(null,null);
    if(!gatewayFirst)root.findExecution("b").signal(null,null);
    assertNotNull("gateway pruned its own concurrent scope; c became unreachable",root.findExecution("c"));
    assertTree(root, Collections.newSetFromMap(new IdentityHashMap<ExecutionImpl, Boolean>()));
    assertFalse(root.isEnded());
    assertTrue(events.isEmpty());
    if (outsideFirst) {
      root.findExecution("after").signal(null,null);
      assertFalse(root.isEnded());
      assertTrue(events.isEmpty());
      assertEquals(Arrays.asList("c"),root.findActiveActivityIds());
      root.findExecution("c").signal(null,null);
      assertTrue(root.isEnded());
      assertFalse(root.isActive());
      assertTrue(root.findActiveActivityIds().isEmpty());
      assertEquals(Arrays.asList("process"),events);
      return;
    }
    root.findExecution("c").signal(null,null);
    assertFalse("outside branch still waiting",root.isEnded());
    assertTrue(events.isEmpty());
    assertEquals(Arrays.asList("after"),root.findActiveActivityIds());
    root.findExecution("after").signal(null,null);
    assertTrue(root.isEnded());
    assertFalse(root.isActive());
    assertTrue(root.findActiveActivityIds().isEmpty());
    assertEquals(Arrays.asList("process"),events);
  }
  private void assertTree(ExecutionImpl node, Set<ExecutionImpl> visited) {
    assertTrue("cycle or duplicate child",visited.add(node));
    for (ExecutionImpl child:node.getExecutions()) {
      assertSame(node,child.getParent());
      assertTree(child,visited);
    }
  }
}
