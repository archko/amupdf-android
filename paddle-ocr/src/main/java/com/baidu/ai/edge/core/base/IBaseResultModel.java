package com.baidu.ai.edge.core.base;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/IBaseResultModel.class */
public interface IBaseResultModel {
    int getLabelIndex();

    void setLabelIndex(int i);

    String getLabel();

    void setLabel(String str);

    float getConfidence();

    void setConfidence(float f);
}
