package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.End;
import pvm.WaitState;
import pvm.ucase.behavior.ParallelGateway;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yurui on 2019/8/15
 * Copyright (c) huidiancloud
 */
public class ParallelGateWayTest extends TestCase {


    public void testProcessEnd() {
        PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
                .createActivity("start")
                .initial()
                .behavior(new Automatic())
                .transition("user")
                .endActivity()
                .createActivity("user")
                .behavior(new WaitState())
                .transition("gate")
                .endActivity()
                .createActivity("gate")
                .behavior(new ParallelGateway())
                .transition("user2")
                .transition("user3")
                .endActivity()
                .createActivity("user2")
                .behavior(new WaitState())
                .transition("end")
                .endActivity()
                .createActivity("user3")
                .behavior(new WaitState())
                .transition("end")
                .endActivity()
                .createActivity("end")
                .behavior(new End())
                .endActivity()
                .buildProcessDefinition();
        PvmProcessInstance processInstance = processDefinition.createProcessInstance();
        processInstance.start();

        PvmExecution activityInstance = processInstance.findExecution("user");
        assertNotNull(activityInstance);

        activityInstance.signal(null, null);

        List<String> assertList = new ArrayList<>();
        assertList.add("user2");
        assertList.add("user3");
        List<String> activeActivityIds = processInstance.findActiveActivityIds();
        assertEquals(assertList, activeActivityIds);

        activityInstance = processInstance.findExecution("user2");
        assertNotNull(activityInstance);
        activityInstance.signal(null, null);

        activityInstance = processInstance.findExecution("user3");
        assertNotNull(activityInstance);
        activityInstance.signal(null, null);

        assertTrue(processInstance.isEnded());
    }

}
