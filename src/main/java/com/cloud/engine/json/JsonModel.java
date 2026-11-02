package com.cloud.engine.json;

import com.cloud.engine.json.elements.Edge;
import com.cloud.engine.json.elements.Node;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yurui
 * @Description: 流程模板的java表述
 * @Date: 17:17 2019/8/6
 * Copyright (c) huidiancloud
*/
@Data
public class JsonModel implements Serializable{
    private String prcDefId;
    private String procKey;
    private String procName;
    private String description;
    private List<Node> nodes;
    private List<Edge> edges;

    /**
     * 返回所有点的连接关系  source_node_id -> out_going_edges
     * @return
     */
    public Map<String, List<Edge>> build(){
        Map<String, List<Edge>> relation = new HashMap<>(nodes.size());
        edges.stream().forEach(edge->{
            String source = edge.getSource();
            if (relation.containsKey(source)){
                relation.get(source).add(edge);
            }else{
                List<Edge> edgeList = new ArrayList<>();
                edgeList.add(edge);
                relation.put(source, edgeList);
            }
        });
        return relation;
    }
}
