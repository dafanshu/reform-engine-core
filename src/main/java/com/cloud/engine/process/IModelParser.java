package com.cloud.engine.process;

import com.cloud.engine.json.JsonModel;
import com.cloud.engine.pvm.PvmProcessDefinition;

/**
 * @author yurui
 * @Description: 用于解析流程模板
 * @Date: 11:43 2019/8/7
 * Copyright (c) huidiancloud
*/
public interface IModelParser {
    /**
     * 解析由前端流程编辑器最终生成的模板json，所以必须保证传入的数据格式正确无误（传入之前做好业务判断）
     * @param jsonModel
     * @param behaviorDetecter
     * @return
     */
    PvmProcessDefinition loads(JsonModel jsonModel, IBehaviorDetecter behaviorDetecter);
}
