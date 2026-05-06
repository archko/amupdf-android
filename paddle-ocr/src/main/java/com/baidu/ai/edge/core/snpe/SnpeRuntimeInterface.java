package com.baidu.ai.edge.core.snpe;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/snpe/SnpeRuntimeInterface.class */
public interface SnpeRuntimeInterface {
    public static final int CPU = 0;
    public static final int GPU = 1;
    public static final int DSP = 2;
    public static final int GPU_FLOAT16 = 3;
    public static final int[] RUNTIMES = {0, 1, 2, 3};
}
