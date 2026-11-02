package pvm.ucase.runtime;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmEvent;
import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.End;
import pvm.WaitState;
import pvm.ucase.EventCollector;
import pvm.ucase.behavior.ParallelGateway;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yurui on 2019/8/19
 * Copyright (c) huidiancloud
 */
public class ForkJoinTest extends TestCase {

    public void testForkJoinWithTransitionName() {
        EventCollector eventCollector = new EventCollector();
        PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
                .setKey("process")
                .setName("name")
                .executionListener(PvmEvent.EVENTNAME_START, eventCollector)
                .executionListener(PvmEvent.EVENTNAME_END, eventCollector)
                .createActivity("start")
                .initial()
                .behavior(new Automatic())
                .transition("user", "edge0")
                .endActivity()
                .createActivity("user")
                .behavior(new WaitState())
                .transition("gate", "edge1")
                .endActivity()
                .createActivity("gate")
                .behavior(new ParallelGateway())
                .transition("user2", "edge2")
                .transition("user3", "edge3")
                .endActivity()
                .createActivity("user2")
                .behavior(new WaitState())
                .transition("end", "edge4")
                .endActivity()
                .createActivity("user3")
                .behavior(new WaitState())
                .transition("end", "edge5")
                .endActivity()
                .createActivity("end")
                .behavior(new End())
                .endActivity()
                .buildProcessDefinition();
        PvmProcessInstance processInstance = processDefinition.createProcessInstance();
        processInstance.start();

        PvmExecution pvmExecution = processInstance.findExecution("user");
        assertNotNull(pvmExecution);

        pvmExecution.signal("edge1", null);

        int userHcode =  pvmExecution.hashCode();

        List<String> executionIds = new ArrayList<>();
        executionIds.add("user2");
        executionIds.add("user3");

        List<String> activeActivityIds = processInstance.findActiveActivityIds();
        assertEquals("expected "+executionIds+", but was \n"+activeActivityIds+"\n", activeActivityIds, executionIds);

        pvmExecution = processInstance.findExecution("gate");
        assertNotNull(pvmExecution);
        // 非并行的时候其实是同一个execution
        assertEquals(userHcode, pvmExecution.hashCode());

        String id = pvmExecution.getActivity().getId();
        assertEquals("gate", id);

        pvmExecution = processInstance.findExecution("user2");
        assertNotNull(pvmExecution);
        // 并行的时候 Execution是新创建出来的
        assertFalse(userHcode == pvmExecution.hashCode());

        pvmExecution.signal("edge4", null);
        pvmExecution = processInstance.findExecution("gate");
        assertNull(pvmExecution);

        pvmExecution = processInstance.findExecution("user3");
        assertNotNull(pvmExecution);
        // 当并行任务只剩下最后一个的时候会用最后的这个任务替换他的父Execution，所以相等
        assertEquals(userHcode, pvmExecution.hashCode());

        pvmExecution.signal("edge5", null);

        assertTrue(processInstance.isEnded());
    }

}
