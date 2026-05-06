package com.baidu.ai.edge.core.pose;

import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.StatisticsResultModelImpl;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/pose/PoseInterface.class */
public interface PoseInterface {
    public static final int POSE_TYPE = 402;

    List<PoseResultModel> pose(Bitmap bitmap) throws BaseException;

    StatisticsResultModelImpl posePro(Bitmap bitmap) throws BaseException;

    void destroy() throws BaseException;
}
