package com.baidu.ai.edge.core.util;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/TimeRecorderNew.class */
public class TimeRecorderNew {

    /* renamed from: a  reason: collision with root package name */
    private long f26a;

    public TimeRecorderNew() {
        restart();
    }

    public void restart() {
        this.f26a = System.currentTimeMillis();
    }

    public long checkpoint() {
        return checkpoint(System.currentTimeMillis());
    }

    public long checkpoint(long j) {
        long j2 = j - this.f26a;
        this.f26a = j;
        return j2;
    }

    public long end() {
        return System.currentTimeMillis() - this.f26a;
    }
}
