package pvm.ucase;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import com.cloud.engine.pvm.delegate.ActivityExecution;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.WaitState;
import pvm.ucase.behavior.EventScopeCreatingSubprocess;

import java.util.List;

/**
 * @author yurui on 2019/7/29
 * Copyright (c) huidiancloud
 */
public class UserTest extends TestCase {

    /**
     *
     *                       create evt scope --+
     *                                          |
     *                                          v
     *
     *           +------------------------------+
     *           | embedded subprocess          |
     * +-----+   |  +-----------+   +---------+ |   +----+   +---+
     * |start|-->|  |startInside|-->|endInside| |-->|wait|-->|end|
     * +-----+   |  +-----------+   +---------+ |   +----+   +---+
     *           +------------------------------+
     *
     *                                                           ^
     *                                                           |
     *                                       destroy evt scope --+
     *
     */
    public void testActivityEndDestroysEventScopes() {
        PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
                .createActivity("start")
                .initial()
                .behavior(new Automatic())
                .transition("embeddedsubprocess")
                .endActivity()
                .createActivity("embeddedsubprocess")
                .scope()
                .behavior(new EventScopeCreatingSubprocess())
                .createActivity("startInside")
                .behavior(new Automatic())
                .transition("endInside")
                .endActivity()
                .createActivity("endInside")
                .behavior(new WaitState())
                .endActivity()
                .transition("wait")
                .endActivity()
                .createActivity("wait")
                .behavior(new WaitState())
                .transition("end")
                .endActivity()
                .createActivity("end")
                .behavior(new Automatic())
                .endActivity()
                .buildProcessDefinition();

        PvmProcessInstance processInstance = processDefinition.createProcessInstance();
        processInstance.start();

        PvmExecution activityInstance = processInstance.findExecution("endInside");
        assertNotNull(activityInstance);

        ActivityExecution activityInstance1 = (ActivityExecution)activityInstance;
        activityInstance1.end();

        boolean eventScopeFound = false;
        List<ExecutionImpl> executions = ((ExecutionImpl)processInstance).getExecutions();
        for (ExecutionImpl executionImpl : executions) {
            if(executionImpl.isEventScope()) {
                eventScopeFound = true;
                break;
            }
        }

        assertTrue(eventScopeFound);

        activityInstance = processInstance.findExecution("wait");
        assertNotNull(activityInstance);

        processInstance.signal(null, null);

        assertTrue(processInstance.isEnded());

    }

}
