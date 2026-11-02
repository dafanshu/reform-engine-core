package com.cloud.engine.enums;

/**
 * @author yurui
 * @Description: 流程图中各元素类型定义
 * @Date: 17:25 2019/8/6
 * Copyright (c) huidiancloud
*/
public enum ShapeEnum {

    CIRCLE_START((byte)1, "开始节点", "circle_start"),
    CIRCLE_END((byte)2, "结束节点", "circle_end"),
    RECT((byte)3, "普通节点", "rect"),
    DIAMOND((byte)4, "分叉节点", "diamond"),
    ELLIPSE((byte)5, "模型节点", "ellipse"),
    SIDE((byte)6, "边", "edge"),
    UNKONW((byte)7, "未知", "unknown");

    private Byte key;
    private String value;
    private String shape;

    ShapeEnum(byte key, String value, String shape) {
        this.key = key;
        this.value = value;
        this.shape = shape;
    }

    public Byte getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getShape() {
        return shape;
    }

    public static ShapeEnum getKey(String shape){
        for(ShapeEnum ee:values()){
            if(ee.getShape().equals(shape)){
                return ee;
            }
        }
        return UNKONW;
    }
}
