package com.baidu.ai.edge.core.classify;

import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.IStatisticsResultModel;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/classify/ClassifyInterface.class */
public interface ClassifyInterface {
    public static final int CLASSIFY_TYPE = 1;

    List<ClassificationResultModel> classify(Bitmap bitmap) throws BaseException;

    List<ClassificationResultModel> classify(Bitmap bitmap, float f) throws BaseException;

    IStatisticsResultModel classifyPro(Bitmap bitmap) throws BaseException;

    void destroy() throws BaseException;
}
