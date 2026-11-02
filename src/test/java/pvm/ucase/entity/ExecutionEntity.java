package pvm.ucase.entity;

import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.process.ProcessDefinitionImpl;
import com.cloud.engine.pvm.runtime.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pvm.Automatic;
import pvm.WaitState;

import java.util.Objects;

/**
 * @author yurui on 2019/7/31
 * Copyright (c) huidiancloud
 */
public class ExecutionEntity extends ExecutionImpl{

    private static Logger log = LoggerFactory.getLogger(ExecutionEntity.class);

    protected String activityId;

    protected String activityName;

    @Override
    protected void ensureProcessDefinitionInitialized() {
        log.debug("init ~~ {}", "ensureProcessDefinitionInitialized");
        if (Objects.isNull(processDefinition)){
            processDefinition = (ProcessDefinitionImpl)new ProcessDefinitionBuilder()
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
        }
    }

    @Override
    protected void ensureParentInitialized() {
        log.debug("init ~~ {}", "ensureParentInitialized");
    }

    @Override
    protected void ensureSuperExecutionInitialized() {
        log.debug("init ~~ {}", "ensureSuperExecutionInitialized");
    }

    @Override
    protected void ensureSubProcessInstanceInitialized() {
        log.debug("init ~~ {}", "ensureSubProcessInstanceInitialized");
    }

    @Override
    protected void ensureProcessInstanceInitialized() {
        log.debug("init ~~ {}", "ensureProcessInstanceInitialized");
        processInstance = this;
    }

    @Override
    protected void ensureActivityInitialized() {
        log.debug("init ~~ {}", "ensureActivityInitialized");
        if (this.activity == null && this.activityId != null) {
            this.activity = this.getProcessDefinition().findActivity(this.activityId);
        }
    }

    @Override
    protected void ensureScopeInitialized() {
        log.debug("init ~~ {}", "ensureScopeInitialized");
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
}
