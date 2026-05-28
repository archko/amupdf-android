package com.baidu.ai.edge.core.classify;

import android.content.Context;
import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.OnlineBase;
import com.baidu.ai.edge.core.base.StatisticsResultModelImpl;
import java.util.List;

@Deprecated
/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/classify/ClassifyOnline.class */
public class ClassifyOnline extends OnlineBase implements ClassifyInterface {
    public ClassifyOnline(String str, String str2, String str3, Context context) {
        super(str, str2, str3);
    }

    @Override // com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap, float f) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.classify.ClassifyInterface
    public StatisticsResultModelImpl classifyPro(Bitmap bitmap) throws BaseException {
        return null;
    }

    @Override // com.baidu.ai.edge.core.classify.ClassifyInterface
    public void destroy() throws BaseException {
    }
}
