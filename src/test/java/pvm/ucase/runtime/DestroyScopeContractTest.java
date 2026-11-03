package pvm.ucase.runtime;
import com.cloud.engine.pvm.*;
import com.cloud.engine.pvm.runtime.*;
import com.cloud.engine.pvm.delegate.*;
import com.cloud.engine.delegate.DelegateExecution;
import pvm.WaitState;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class DestroyScopeContractTest {
  private ExecutionImpl process(List<String> events) { return process(events,false); }
  private ExecutionImpl process(List<String> events, boolean scoped) {
    PvmProcessDefinition child = new ProcessDefinitionBuilder("child")
      .executionListener(PvmEvent.EVENTNAME_END,e->events.add("child-process"))
      .createActivity("child-wait").initial().behavior(new WaitState())
      .executionListener(PvmEvent.EVENTNAME_END,e->events.add("child-wait")).endActivity().buildProcessDefinition();
    ProcessDefinitionBuilder builder=new ProcessDefinitionBuilder("parent")
      .createActivity("fork").initial().behavior(e->{e.inactivate(); e.takeAll(e.getActivity().getOutgoingTransitions(),null);})
      .transition("call").transition("wait").endActivity()
      .createActivity("call");
    if(scoped)builder.scope();
    ExecutionImpl root=(ExecutionImpl)builder.behavior(new SubProcessActivityBehavior(){
        public void execute(ActivityExecution e){e.createSubProcessInstance(child).start();}
        public void completing(DelegateExecution e,DelegateExecution c){events.add("completing");}
        public void completed(ActivityExecution e){events.add("completed");e.end();}
      }).endActivity().createActivity("wait").behavior(new WaitState()).endActivity()
      .createActivity("continued").behavior(new WaitState()).endActivity()
      .buildProcessDefinition().createProcessInstance();
    root.start(); return root;
  }
  @Test public void destroyScopeCancelsChildOnce(){
    List<String> events=new ArrayList<>(); ExecutionImpl root=process(events);
    root.destroyScope("cancel");
    assertTrue(root.getExecutions().isEmpty());
    assertFalse(root.isEnded());
    // The fork left the carrier inactive; the interruption handler chooses its continuation.
    root.setActive(true);
    root.executeActivity(root.getProcessDefinition().findActivity("continued"));
    assertEquals(Arrays.asList("continued"),root.findActiveActivityIds());
    root.signal(null,null);
    assertTrue(root.isEnded());
    assertEquals(Arrays.asList("child-wait","child-process"),events);
  }
  @Test public void deleteCascadeControl(){
    List<String> events=new ArrayList<>(); ExecutionImpl root=process(events);
    root.deleteCascade("cancel");
    assertEquals(Arrays.asList("child-wait","child-process"),events);
  }
  @Test public void destroyCallingScopeCancelsOwnSubprocess(){
    List<String> events=new ArrayList<>(); ExecutionImpl root=process(events);
    ExecutionImpl call=root.findExecution("call");
    ExecutionImpl subprocess=call.getSubProcessInstance();
    call.destroyScope("interrupt-call");
    assertSame(call,root.findExecution("call"));
    assertNotNull(root.findExecution("wait"));
    assertFalse(root.isEnded());
    assertTrue(subprocess.findActiveActivityIds().isEmpty());
    assertEquals(Arrays.asList("child-wait","child-process"),events);
  }
  @Test public void destroyParentWithScopedCall() {
    List<String> events=new ArrayList<>(); ExecutionImpl root=process(events,true);
    root.destroyScope("cancel");
    assertTrue(root.getExecutions().isEmpty());
    assertEquals(Arrays.asList("child-wait","child-process"),events);
    assertFalse(root.isEnded());
  }
  @Test public void destroyScopedCallOwnSubprocess() {
    List<String> events=new ArrayList<>(); ExecutionImpl root=process(events,true);
    ExecutionImpl call=root.findExecution("call");
    ExecutionImpl subprocess=call.getSubProcessInstance();
    call.destroyScope("cancel");
    assertEquals(Arrays.asList("child-wait","child-process"),events);
    assertTrue(subprocess.findActiveActivityIds().isEmpty());
    assertSame(call,root.findExecution("call"));
    assertFalse(root.isEnded());
  }
}
