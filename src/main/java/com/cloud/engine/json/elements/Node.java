package com.cloud.engine.json.elements;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yurui
 * @Description: 用于解析json中描述的点
 * @Date: 17:30 2019/8/6
 * Copyright (c) huidiancloud
*/
@Data
public class Node implements Serializable {
    private String id;
    private String shape;
    private String label;
    /** 这里冗余一个字段方便直接提取内存模型中点线之间的关系 **/
    private transient String edgeId;
}
