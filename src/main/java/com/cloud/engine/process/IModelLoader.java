package com.cloud.engine.process;

import com.cloud.engine.json.JsonModel;

/**
 * @author yurui
 * @Description: 用于流程模型查询
 * @Date: 11:58 2019/8/7
 * Copyright (c) huidiancloud
*/
public interface IModelLoader {

    JsonModel searchByKey(String procKey);

}
