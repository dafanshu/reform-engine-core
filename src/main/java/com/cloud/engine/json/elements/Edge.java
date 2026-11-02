package com.cloud.engine.json.elements;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yurui
 * @Description: 用于解析json中描述的线
 * @Date: 17:32 2019/8/6
 * Copyright (c) huidiancloud
*/
@Data
public class Edge implements Serializable{
    private String id;
    private String target;
    private String source;
}
