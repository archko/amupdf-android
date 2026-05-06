package com.baidu.ai.edge.core.snpe;

import com.baidu.ai.edge.core.base.IBaseConfig;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/snpe/ISnpeConfig.class */
public interface ISnpeConfig extends IBaseConfig {
    boolean isAutocheckQcom();

    int[] getSnpeRuntimesOrder();
}
