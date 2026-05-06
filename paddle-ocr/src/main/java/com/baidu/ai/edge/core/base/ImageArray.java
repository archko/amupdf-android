package com.baidu.ai.edge.core.base;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/ImageArray.class */
public class ImageArray {

    /* renamed from: a  reason: collision with root package name */
    private float[] f14a;
    private int b;

    public ImageArray(float[] fArr) {
        this(fArr, 0);
    }

    public ImageArray(float[] fArr, int i) {
        this.f14a = fArr;
        this.b = i;
    }

    public int a() {
        return this.b;
    }

    public void a(int i) {
        this.b = i;
    }

    public float[] b() {
        return this.f14a;
    }
}
