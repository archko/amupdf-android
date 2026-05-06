package com.baidu.ai.edge.core.base;

import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/IStatisticsResultModel.class */
public interface IStatisticsResultModel {
    long getPostprocessTime();

    void setPostprocessTime(long j);

    long getPreprocessTime();

    void setPreprocessTime(long j);

    long getForwardTime();

    void setForwardTime(long j);

    List<? extends IBaseResultModel> getResultModel();

    void setResultModel(List<? extends IBaseResultModel> list);
}
