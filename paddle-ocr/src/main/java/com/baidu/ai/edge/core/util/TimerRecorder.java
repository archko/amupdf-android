package com.baidu.ai.edge.core.util;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/TimerRecorder.class */
public class TimerRecorder {

    /* renamed from: a  reason: collision with root package name */
    private static long f27a;

    public static synchronized void start() {
        f27a = System.currentTimeMillis();
    }

    public static synchronized long end() {
        return System.currentTimeMillis() - f27a;
    }
}
