package com.cloud.engine.process.impl;

import com.cloud.engine.json.JsonModel;
import com.cloud.engine.json.elements.Edge;
import com.cloud.engine.json.elements.Node;
import com.cloud.engine.process.IBehaviorDetecter;
import com.cloud.engine.process.IModelParser;
import com.cloud.engine.pvm.ProcessDefinitionBuilder;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.delegate.ActivityBehavior;

import java.util.List;
import java.util.Map;

/**
 * @author yurui
 * @Description: 将json格式的流程模板解析为pvm内存模型，注意这里只解析点和线，不关心其他任何的业务细节
 * @Date: 17:03 2019/8/6
 * Copyright (c) huidiancloud
*/
public class SimpleModelParserImpl implements IModelParser{

    /**
     * 解析由前端流程编辑器最终生成的模板json，所以必须保证传入的数据格式正确无误（传入之前做好业务判断）
     * @param jsonModel
     * @param behaviorDetecter
     * @return
     */
    @Override
    public PvmProcessDefinition loads(JsonModel jsonModel, IBehaviorDetecter behaviorDetecter){
        String prcDefId = jsonModel.getPrcDefId();
        Map<String, List<Edge>> relation = jsonModel.build();
        List<Node> sourceNodes = jsonModel.getNodes();
        ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder(prcDefId)
                .setKey(jsonModel.getProcKey())
                .setName(jsonModel.getProcName())
                .setDescription(jsonModel.getDescription());
        for (int i = 0,j=sourceNodes.size(); i < j; i++) {
            Node source = sourceNodes.get(i);
            String activityId = source.getId();
            processDefinitionBuilder.createActivity(activityId);
            if (i==0){
                processDefinitionBuilder.initial();
            }
            List<Edge> outgoingEdges = relation.get(activityId);
            if (outgoingEdges!=null && !outgoingEdges.isEmpty()){
                for (Edge out:outgoingEdges){
                    processDefinitionBuilder.transition(out.getTarget(), out.getId());
                }
            }
            ActivityBehavior behavior = behaviorDetecter.getBehavior(source.getShape());
            processDefinitionBuilder.behavior(behavior).endActivity();
        }
        return processDefinitionBuilder.buildProcessDefinition();
    }

}
