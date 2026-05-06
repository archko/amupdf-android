package com.baidu.ai.edge.core.pose;

import android.graphics.Point;
import com.baidu.ai.edge.core.base.IBaseResultModel;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/pose/IPoseResultModel.class */
public interface IPoseResultModel extends IBaseResultModel {
    List<IPoseResultModel> getPairs();

    int getIndex();

    Point getPoint();
}
