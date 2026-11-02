package pvm;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.process.ActivityImpl;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import junit.framework.TestCase;
import pvm.ucase.EventCollector;

public class PvmTest extends TestCase {


  public void testProcessEnd() {
    EventCollector eventCollector = new EventCollector();
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
      .initial()
      .behavior(new WaitState())
      .executionListener(PvmEvent.EVENTNAME_TAKE, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .transition("b")
      .endActivity()
      .createActivity("b")
      .behavior(new WaitState())
      .transition("c")
      .endActivity()
      .createActivity("c")
      .behavior(new End())
      .endActivity()
      .buildProcessDefinition();
    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    PvmExecution activityInstance = processInstance.findExecution("a");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("b");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    assertTrue(processInstance.isEnded());

    System.out.println(eventCollector.events);
  }


  public void testPvmSubProcess(){
    ProcessDefinitionImpl processDefinition = new ProcessDefinitionImpl("process");
    ActivityImpl initNode = processDefinition.createActivity("aa");
    processDefinition.setInitial(initNode);


  }

  public void testPvmWaitState() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
        .initial()
        .behavior(new WaitState())
        .transition("b")
      .endActivity()
      .createActivity("b")
        .behavior(new WaitState())
        .transition("c")
      .endActivity()
      .createActivity("c")
        .behavior(new WaitState())
        .transition("sub")
      .endActivity()
      .createActivity("sub").behavior(new WaitState())
            .createActivity("sub-start")
            .behavior(new WaitState())
            .transition("sub-a")
            .endActivity()
            .createActivity("sub-a")
            .behavior(new WaitState())
            .endActivity().endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    PvmExecution activityInstance = processInstance.findExecution("a");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("b");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("c");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("sub");
    assertNotNull(activityInstance);

    System.out.println(processInstance.isEnded());
  }

  public void testPvmAutomatic() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
        .initial()
        .behavior(new Automatic())
        .transition("b")
      .endActivity()
      .createActivity("b")
        .behavior(new Automatic())
        .transition("c")
      .endActivity()
        .createActivity("c")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertNotNull(processInstance.findExecution("c"));
  }

  public void testPvmDecision() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("checkCredit")
      .endActivity()
      .createActivity("checkCredit")
        .behavior(new Decision())
        .transition("askDaughterOut", "wow")
        .transition("takeToGolf", "nice")
        .transition("ignore", "default")
      .endActivity()
      .createActivity("takeToGolf")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("askDaughterOut")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("ignore")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "Aaa-");
    processInstance.start();
    assertNotNull(processInstance.findExecution("takeToGolf"));

    processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "AAA+");
    processInstance.start();
    assertNotNull(processInstance.findExecution("askDaughterOut"));

    processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "bb-");
    processInstance.start();
    assertNotNull(processInstance.findExecution("ignore"));
  }
}
