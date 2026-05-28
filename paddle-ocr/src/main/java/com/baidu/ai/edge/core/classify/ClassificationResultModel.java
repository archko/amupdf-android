package com.baidu.ai.edge.core.classify;

import com.baidu.ai.edge.core.base.BaseResultModel;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/classify/ClassificationResultModel.class */
public class ClassificationResultModel extends BaseResultModel implements IClassificationResultModel {
    public ClassificationResultModel(String str, float f) {
        super(str, f);
    }

    public ClassificationResultModel(int i, float f) {
        super("not loaded", f);
        this.f11a = i;
    }
}
