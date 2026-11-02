package pvm.ucase;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.End;
import pvm.WaitState;
import pvm.ucase.behavior.ParallelGateway;

import java.util.ArrayList;
import java.util.List;

public class PvmScopesAndConcurrencyTest extends TestCase {

  /**
   *         +---------+
   *         |scope    |  +--+
   *         |      +---->|c1|
   *         |      |  |  +--+
   *         |      |  |
   * +-----+ |  +----+ |  +--+
   * |start|--->|fork|--->|c2|
   * +-----+ |  +----+ |  +--+
   *         |      |  |
   *         |      |  |  +--+
   *         |      +---->|c3|
   *         |         |  +--+
   *         +---------+
   */
  public void testConcurrentPathsComingOutOfScope() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("fork")
      .endActivity()
      .createActivity("scope")
        .scope()
        .createActivity("fork")
          .behavior(new ParallelGateway())
          .transition("c1")
          .transition("c2")
          .transition("c3")
        .endActivity()
      .endActivity()
      .createActivity("c1")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("c2")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("c3")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> activeActivityIds = processInstance.findActiveActivityIds();
    List<String> expectedActiveActivityIds = new ArrayList<String>();
    expectedActiveActivityIds.add("c3");
    expectedActiveActivityIds.add("c1");
    expectedActiveActivityIds.add("c2");

    assertEquals(expectedActiveActivityIds, activeActivityIds);

    for (String id:activeActivityIds){
      ActivityExecution execution = (ActivityExecution)processInstance.findExecution(id);
      execution.end();
    }

    assertTrue(processInstance.isEnded());

  }

  /**
   *                      +------------+
   *                      |scope       |
   *                  +----------+     |
   *                  |   |      v     |
   * +-----+   +--------+ |   +------+ |
   * |start|-->|parallel|---->|inside| |
   * +-----+   +--------+ |   +------+ |
   *                  |   |      ^     |
   *                  +----------+     |
   *                      |            |
   *                      +------------+
   */
  public void testConcurrentPathsGoingIntoScope() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("parallel")
      .endActivity()
      .createActivity("parallel")
        .behavior(new ParallelGateway())
        .transition("inside")
        .transition("inside")
        .transition("inside")
      .endActivity()
      .createActivity("scope")
        .scope()
        .createActivity("inside")
          .behavior(new WaitState())
        .endActivity()
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> activeActivityIds = processInstance.findActiveActivityIds();
    List<String> expectedActiveActivityIds = new ArrayList<String>();
    expectedActiveActivityIds.add("inside");
    expectedActiveActivityIds.add("inside");
    expectedActiveActivityIds.add("inside");

    assertEquals(expectedActiveActivityIds, activeActivityIds);
  }

  /**
   *                      +---------+
   *                      | +----+  |
   *                  +---->| c1 |------+
   *                  |   | +----+  |   v
   * +-------+   +------+ |         | +------+   +-----+
   * | start |-->| fork | | noscope | | join |-->| end |
   * +-------+   +------+ |         | +------+   +-----+
   *                  |   | +----+  |   ^
   *                  +---->| c2 |------+
   *                      | +----+  |
   *                      +---------+
   */
  public void testConcurrentPathsThroughNonScopeNestedActivity() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("scopes and concurrency")
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("fork")
      .endActivity()
      .createActivity("fork")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("c1")
        .transition("c2")
      .endActivity()
      .createActivity("noscope")
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("c1")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
        .createActivity("c2")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
      .endActivity()
      .createActivity("join")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add("start on ProcessDefinition(scopes and concurrency)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(fork)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(noscope)");
    expectedEvents.add("start on Activity(c1)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(noscope)");
    expectedEvents.add("start on Activity(c2)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    PvmExecution execution = processInstance.findExecution("c1");

    List<String> activeActivityIds = processInstance.findActiveActivityIds();

    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c1)");
    expectedEvents.add("end on Activity(noscope)");
    expectedEvents.add("start on Activity(join)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    execution = processInstance.findExecution("c2");
    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c2)");
    expectedEvents.add("end on Activity(noscope)");
    expectedEvents.add("start on Activity(join)");
    expectedEvents.add("end on Activity(join)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(scopes and concurrency)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    assertTrue(processInstance.isEnded());
  }

  /**
   *                      +---------+
   *                      | +----+  |
   *                  +---->| c1 |------+
   *                  |   | +----+  |   v
   * +-------+   +------+ |         | +------+   +-----+
   * | start |-->| fork | |  scope  | | join |-->| end |
   * +-------+   +------+ |         | +------+   +-----+
   *                  |   | +----+  |   ^
   *                  +---->| c2 |------+
   *                      | +----+  |
   *                      +---------+
   */
  public void testConcurrentPathsThroughScope() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("scopes and concurrency")
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("fork")
      .endActivity()
      .createActivity("fork")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("c1")
        .transition("c2")
      .endActivity()
      .createActivity("scope")
        .scope()
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("c1")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
        .createActivity("c2")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
      .endActivity()
      .createActivity("join")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();
    String s = processDefinition.toString();
    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add("start on ProcessDefinition(scopes and concurrency)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(fork)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(scope)");
    expectedEvents.add("start on Activity(c1)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(scope)");
    expectedEvents.add("start on Activity(c2)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    PvmExecution execution = processInstance.findExecution("c1");

    List<String> activeActivityIds = processInstance.findActiveActivityIds();

    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c1)");
    expectedEvents.add("end on Activity(scope)");
    expectedEvents.add("start on Activity(join)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    execution = processInstance.findExecution("c2");
    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c2)");
    expectedEvents.add("end on Activity(scope)");
    expectedEvents.add("start on Activity(join)");
    expectedEvents.add("end on Activity(join)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(scopes and concurrency)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    assertTrue(processInstance.isEnded());
  }

  /**
   *           +--------------------+
   *           |            +----+  |
   *           |      +---->| c1 |------+
   *           |      |     +----+  |   v
   * +-------+ | +------+           | +------+   +-----+
   * | start |-->| fork |    scope  | | join |-->| end |
   * +-------+ | +------+           | +------+   +-----+
   *           |      |     +----+  |   ^
   *           |      +---->| c2 |------+
   *           |            +----+  |
   *           +--------------------+
   */
  public void testConcurrentPathsGoingOutOfScope() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("scopes and concurrency")
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("fork")
      .endActivity()
      .createActivity("scope")
        .scope()
        .createActivity("fork")
          .behavior(new ParallelGateway())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("c1")
          .transition("c2")
        .endActivity()
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("c1")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
        .createActivity("c2")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
      .endActivity()
      .createActivity("join")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add("start on ProcessDefinition(scopes and concurrency)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(scope)");
    expectedEvents.add("start on Activity(fork)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(c1)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(c2)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    PvmExecution execution = processInstance.findExecution("c1");
    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c1)");
    expectedEvents.add("end on Activity(scope)");
    expectedEvents.add("start on Activity(join)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    execution = processInstance.findExecution("c2");
    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c2)");
    expectedEvents.add("end on Activity(scope)");
    expectedEvents.add("start on Activity(join)");
    expectedEvents.add("end on Activity(join)");
    expectedEvents.add("start on Activity(end)");
    expectedEvents.add("end on Activity(end)");
    expectedEvents.add("end on ProcessDefinition(scopes and concurrency)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    assertTrue(processInstance.isEnded());
  }

  /**
   *                      +--------------------+
   *                      | +----+             |
   *                  +---->| c1 |------+      |
   *                  |   | +----+      v      |
   * +-------+   +------+ |           +------+ | +-----+
   * | start |-->| fork | |  scope    | join |-->| end |
   * +-------+   +------+ |           +------+ | +-----+
   *                  |   | +----+      ^      |
   *                  +---->| c2 |------+      |
   *                      | +----+             |
   *                      +--------------------+
   */
  public void testConcurrentPathsJoiningInsideScope() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder("scopes and concurrency")
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("fork")
      .endActivity()
      .createActivity("fork")
        .behavior(new ParallelGateway())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .transition("c1")
        .transition("c2")
      .endActivity()
      .createActivity("scope")
        .scope()
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
        .createActivity("c1")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
        .createActivity("c2")
          .behavior(new WaitState())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("join")
        .endActivity()
        .createActivity("join")
          .behavior(new ParallelGateway())
          .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
          .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
          .transition("end")
        .endActivity()
      .endActivity()
      .createActivity("end")
        .behavior(new End())
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add("start on ProcessDefinition(scopes and concurrency)");
    expectedEvents.add("start on Activity(start)");
    expectedEvents.add("end on Activity(start)");
    expectedEvents.add("start on Activity(fork)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(scope)");
    expectedEvents.add("start on Activity(c1)");
    expectedEvents.add("end on Activity(fork)");
    expectedEvents.add("start on Activity(scope)");
    expectedEvents.add("start on Activity(c2)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    PvmExecution execution = processInstance.findExecution("c1");
    execution.signal(null, null);

    expectedEvents = new ArrayList<String>();
    expectedEvents.add("end on Activity(c1)");
    expectedEvents.add("start on Activity(join)");

    assertEquals("expected "+expectedEvents+", but was \n"+eventCollector+"\n", expectedEvents, eventCollector.events);
    eventCollector.events.clear();

    execution = processInstance.findExecution("c2");

    // this process gets blocked in the join
    execution.signal(null, null);
  }
}
