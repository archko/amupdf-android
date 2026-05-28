package com.baidu.ai.edge.core.segment;

import android.graphics.Rect;
import com.baidu.ai.edge.core.base.IBaseResultModel;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/segment/ISegmentationResultModel.class */
public interface ISegmentationResultModel extends IBaseResultModel {
    void setBox(Rect rect);

    byte[] getMask();

    String getMaskLEcode();

    void setMask(byte[] bArr);

    Rect getBox();
}
