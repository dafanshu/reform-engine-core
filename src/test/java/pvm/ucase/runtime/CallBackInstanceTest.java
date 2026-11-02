package pvm.ucase.runtime;

import com.cloud.engine.pvm.PvmExecution;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import junit.framework.TestCase;
import pvm.ucase.entity.ExecutionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yurui on 2019/7/31
 * Copyright (c) huidiancloud
 */
public class CallBackInstanceTest extends TestCase {


    public void testCallFromActivity() {
        ExecutionEntity executionEntity = new ExecutionEntity();
        executionEntity.setActive(true);
        executionEntity.setConcurrent(false);
        executionEntity.setScope(false);
        executionEntity.setActivityId("c");

        ExecutionImpl processInstance = executionEntity.getProcessInstance();

        List<String> expectedActiveActivityIds = new ArrayList<String>();
        expectedActiveActivityIds.add("c");

        List<String> activeActivityIds = processInstance.findActiveActivityIds();
        assertEquals(expectedActiveActivityIds, activeActivityIds);

        String currentActivit = activeActivityIds.get(0);
        PvmExecution currentExecution = processInstance.findExecution("c");
        currentExecution.signal(null, null);

        assertTrue(processInstance.isEnded());
    }

}
