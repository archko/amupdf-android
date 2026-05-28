package com.baidu.ai.edge.core.detect;

import android.content.Context;
import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.IStatisticsResultModel;
import com.baidu.ai.edge.core.base.OnlineBase;
import java.util.List;

@Deprecated
/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/detect/DetectOnline.class */
public class DetectOnline extends OnlineBase implements DetectInterface {
    public DetectOnline(String str, String str2, String str3, Context context) {
        super(str, str2, str3);
    }

    @Override // com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap, float f) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.detect.DetectInterface
    public IStatisticsResultModel detectPro(Bitmap bitmap) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.detect.DetectInterface
    public void destroy() throws BaseException {
    }
}
