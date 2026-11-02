/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pvm.ucase;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.End;
import pvm.WaitState;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Tom Baeyens
 */
public class PvmScopeWaitStateTest extends TestCase {

  /**
   * +-----+   +----------+   +---+
   * |start|-->|scopedWait|-->|end|
   * +-----+   +----------+   +---+
   */
  public void testWaitStateScope() {
    EventCollector eventCollector = new EventCollector();
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("scopedWait")
        .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
      .createActivity("scopedWait")
//        .scope()
        .behavior(new WaitState())
        .transition("end")
      .endActivity()
      .createActivity("end")
        .behavior(new End())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    PvmExecution execution = processInstance.findExecution("scopedWait");
    assertNotNull(execution);

    execution.signal(null, null);

    assertEquals(new ArrayList<String>(), processInstance.findActiveActivityIds());
    assertTrue(processInstance.isEnded());
  }

  /**
   *          +--------------+
   *          | outerScope   |
   * +-----+  | +----------+ |  +---+
   * |start|--->|scopedWait|--->|end|
   * +-----+  | +----------+ |  +---+
   *          +--------------+
   */
  public void testNestedScope() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("scopedWait")
      .endActivity()
      .createActivity("outerScope")
        .scope()
        .createActivity("scopedWait")
          .scope()
          .behavior(new WaitState())
          .transition("end")
        .endActivity()
      .endActivity()
      .createActivity("end")
        .behavior(new End())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    List<String> activeActivityIds = processInstance.findActiveActivityIds();
    for (String id:activeActivityIds){
      ExecutionImpl execution = (ExecutionImpl) processInstance.findExecution(id);
      execution.getActivity();
    }

    PvmExecution activityInstance = processInstance.findExecution("scopedWait");
    assertNotNull(activityInstance);

    activityInstance.signal(null, null);

    assertEquals(new ArrayList<String>(), processInstance.findActiveActivityIds());
    assertTrue(processInstance.isEnded());
  }
}
