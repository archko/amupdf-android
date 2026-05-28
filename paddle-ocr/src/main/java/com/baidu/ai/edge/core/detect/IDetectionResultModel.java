package com.baidu.ai.edge.core.detect;

import android.graphics.Rect;
import com.baidu.ai.edge.core.base.IBaseResultModel;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/detect/IDetectionResultModel.class */
public interface IDetectionResultModel extends IBaseResultModel {
    void setBounds(Rect rect);

    Rect getBounds();
}
