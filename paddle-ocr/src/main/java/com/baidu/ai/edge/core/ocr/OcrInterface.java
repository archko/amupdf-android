package com.baidu.ai.edge.core.ocr;

import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ocr/OcrInterface.class */
public interface OcrInterface {
    public static final int OCR_TYPE = 100;

    List<OcrResultModel> ocr(Bitmap bitmap) throws BaseException;

    List<OcrResultModel> ocr(Bitmap bitmap, float f) throws BaseException;

    void destroy() throws BaseException;
}
