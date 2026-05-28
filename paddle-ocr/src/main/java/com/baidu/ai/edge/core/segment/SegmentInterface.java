package com.baidu.ai.edge.core.segment;

import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.StatisticsResultModelImpl;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/segment/SegmentInterface.class */
public interface SegmentInterface {
    public static final int SEGEMENT_TYPE = 6;

    List<SegmentationResultModel> segment(Bitmap bitmap) throws BaseException;

    List<SegmentationResultModel> segment(Bitmap bitmap, float f) throws BaseException;

    StatisticsResultModelImpl segmentPro(Bitmap bitmap) throws BaseException;

    void destroy() throws BaseException;
}
