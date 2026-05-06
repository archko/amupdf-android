package com.baidu.ai.edge.core.base;

import com.baidu.ai.edge.core.util.ImageUtil;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/JniParamBuilder.class */
public class JniParamBuilder {
    public static JniParam a(PreprocessConfig preprocessConfig, int i, int i2) {
        JniParam jniParam = new JniParam();
        jniParam.put("width", i);
        jniParam.put("height", i2);
        jniParam.put("prepWidth", preprocessConfig.q());
        jniParam.put("prepHeight", preprocessConfig.p());
        jniParam.put("padded_width", ImageUtil.calPaddedValue(i, preprocessConfig.o()));
        jniParam.put("padded_height", ImageUtil.calPaddedValue(i2, preprocessConfig.o()));
        jniParam.put("imgMeans", preprocessConfig.f());
        jniParam.put("scales", preprocessConfig.s());
        jniParam.put("colorFormat", preprocessConfig.d());
        jniParam.put("rescaleMode", preprocessConfig.r());
        jniParam.put("isWarpAffineKeepRes", Boolean.valueOf(preprocessConfig.x()));
        jniParam.put("channelOrder", preprocessConfig.c());
        jniParam.put("isRGB", Boolean.valueOf(preprocessConfig.d().equals("RGB")));
        jniParam.put("isHWC", Boolean.valueOf(preprocessConfig.c().equals("HWC")));
        jniParam.put("paddings", preprocessConfig.o());
        jniParam.put("isLetterbox", Boolean.valueOf(preprocessConfig.u()));
        jniParam.put("isSkipNorm", Boolean.valueOf(preprocessConfig.w()));
        jniParam.put("centerCropWidth", preprocessConfig.b());
        jniParam.put("centerCropHeight", preprocessConfig.a());
        jniParam.put("isPadding", Boolean.valueOf(preprocessConfig.v()));
        jniParam.put("paddingMode", preprocessConfig.m());
        jniParam.put("paddingFillWidth", preprocessConfig.l());
        jniParam.put("paddingFillHeight", preprocessConfig.k());
        jniParam.put("paddingScalar", preprocessConfig.n());
        return jniParam;
    }
}
