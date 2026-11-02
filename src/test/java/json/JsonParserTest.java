package json;

import com.alibaba.fastjson.JSON;
import com.cloud.engine.enums.ShapeEnum;
import com.cloud.engine.json.JsonModel;
import com.cloud.engine.json.elements.Edge;
import com.cloud.engine.json.elements.Node;
import com.cloud.engine.process.IBehaviorDetecter;
import com.cloud.engine.process.IModelParser;
import com.cloud.engine.process.impl.SimpleBehaviorDetecterImpl;
import com.cloud.engine.process.impl.SimpleModelParserImpl;
import com.cloud.engine.pvm.PvmProcessDefinition;
import com.cloud.engine.pvm.PvmProcessInstance;
import junit.framework.TestCase;
import pvm.Automatic;
import pvm.End;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yurui on 2019/8/6
 * Copyright (c) huidiancloud
 */
public class JsonParserTest extends TestCase {

    public void testParseModel() {
        String json = getJson();
        JsonModel jsonModel = JSON.parseObject(json, JsonModel.class);
        IBehaviorDetecter behaviorDetecter = getBehaviorDetecter();
        IModelParser parser = new SimpleModelParserImpl();
        PvmProcessDefinition processDefinition = parser.loads(jsonModel, behaviorDetecter);
        PvmProcessInstance processInstance = processDefinition.createProcessInstance();
        processInstance.start();
        assertTrue(processInstance.isEnded());
    }

    private String getJson(){
        JsonModel model = new JsonModel();
        model.setPrcDefId("10000");
        model.setProcKey("process");
        model.setProcName("测试流程");
        model.setDescription("描述");
        List<Node> nodes = new ArrayList<>();
        Node startNode = new Node();
        startNode.setId("start");
        startNode.setLabel("开始节点");
        startNode.setShape(ShapeEnum.CIRCLE_START.getShape());
        nodes.add(startNode);

        Node userNode = new Node();
        userNode.setId("user1");
        userNode.setLabel("用户任务环节1");
        userNode.setShape(ShapeEnum.RECT.getShape());
        nodes.add(userNode);

        Node gateNode = new Node();
        gateNode.setId("gate");
        gateNode.setLabel("网关");
        gateNode.setShape(ShapeEnum.DIAMOND.getShape());
        nodes.add(gateNode);

        Node endNode = new Node();
        endNode.setId("end");
        endNode.setLabel("结束环节");
        endNode.setShape(ShapeEnum.DIAMOND.getShape());
        nodes.add(endNode);

        model.setNodes(nodes);

        List<Edge> edges = new ArrayList<>();
        Edge edge1 = new Edge();
        edge1.setId("edge1");
        edge1.setSource("start");
        edge1.setTarget("user1");
        edges.add(edge1);

        Edge edge2 = new Edge();
        edge2.setId("edge2");
        edge2.setSource("user1");
        edge2.setTarget("gate");
        edges.add(edge2);

        Edge edge3 = new Edge();
        edge3.setId("edge3");
        edge3.setSource("gate");
        edge3.setTarget("end");
        edges.add(edge3);

        model.setEdges(edges);

        return JSON.toJSONString(model);
    }

    private IBehaviorDetecter getBehaviorDetecter(){
        IBehaviorDetecter behaviorDetecter = new SimpleBehaviorDetecterImpl();
        behaviorDetecter.put(ShapeEnum.CIRCLE_START.getShape(), new Automatic());
        behaviorDetecter.put(ShapeEnum.RECT.getShape(), new Automatic());
        behaviorDetecter.put(ShapeEnum.DIAMOND.getShape(), new Automatic());
        behaviorDetecter.put(ShapeEnum.CIRCLE_END.getShape(), new End());
        return behaviorDetecter;
    }
}
