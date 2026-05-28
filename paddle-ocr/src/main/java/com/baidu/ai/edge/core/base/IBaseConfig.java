package com.baidu.ai.edge.core.base;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/IBaseConfig.class */
public interface IBaseConfig {
    int getMid();

    int getRid();

    String getProduct();

    String getSoc();

    String getModelFileAssetPath();

    void setModelFileAssetPath(String str);

    boolean isAcceleration();

    String getUserDeviceId();

    String getAuthMode();

    long getAuthInterval();

    String getAuthDomain();

    String getDeviceLicenseUri();
}
