package com.baidu.ai.edge.core.base;

import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/StatisticsResultModelImpl.class */
public class StatisticsResultModelImpl implements IStatisticsResultModel {

    /* renamed from: a  reason: collision with root package name */
    private List<? extends IBaseResultModel> f16a;
    private long b = 0;
    private long c = 0;
    private long d = 0;

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public long getPostprocessTime() {
        return this.d;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public void setPostprocessTime(long j) {
        this.d = j;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public long getPreprocessTime() {
        return this.b;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public void setPreprocessTime(long j) {
        this.b = j;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public long getForwardTime() {
        return this.c;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public void setForwardTime(long j) {
        this.c = j;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public List<? extends IBaseResultModel> getResultModel() {
        return this.f16a;
    }

    @Override // com.baidu.ai.edge.core.base.IStatisticsResultModel
    public void setResultModel(List<? extends IBaseResultModel> list) {
        this.f16a = list;
    }
}
