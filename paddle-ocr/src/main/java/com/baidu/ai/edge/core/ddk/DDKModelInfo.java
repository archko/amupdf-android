package com.baidu.ai.edge.core.ddk;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DDKModelInfo.class */
public class DDKModelInfo {

    /* renamed from: a  reason: collision with root package name */
    private boolean f19a;
    private int b;
    private int c;
    private int d;
    private int e;
    private int f;
    private int g;
    private int h;
    private int i;
    private int j;
    private int k;

    public DDKModelInfo() {
        this.b = 1;
        this.c = 3;
        this.d = 0;
        this.e = 0;
        this.f = 1;
        this.g = 1;
        this.h = 0;
        this.i = 1;
        this.j = 1;
        this.k = 1;
        this.f19a = false;
    }

    public DDKModelInfo(int i, int i2, int i3) {
        this.b = 1;
        this.c = 3;
        this.d = i;
        this.e = i2;
        this.f = 1;
        this.g = 1;
        this.h = i3;
        this.i = 1;
        this.j = 1;
        this.k = 1;
        this.f19a = false;
    }

    public boolean isAddSoftmaxFlag() {
        return this.f19a;
    }

    public void setAddSoftmaxFlag(boolean z) {
        this.f19a = z;
    }

    public int getInput_N() {
        return this.b;
    }

    public void setInput_N(int i) {
        this.b = i;
    }

    public int getInput_C() {
        return this.c;
    }

    public void setInput_C(int i) {
        this.c = i;
    }

    public int getInput_H() {
        return this.d;
    }

    public void setInput_H(int i) {
        this.d = i;
    }

    public int getInput_W() {
        return this.e;
    }

    public void setInput_W(int i) {
        this.e = i;
    }

    public int getInput_Number() {
        return this.f;
    }

    public void setInput_Number(int i) {
        this.f = i;
    }

    public int getOutput_N() {
        return this.g;
    }

    public void setOutput_N(int i) {
        this.g = i;
    }

    public int getOutput_C() {
        return this.h;
    }

    public void setOutput_C(int i) {
        this.h = i;
    }

    public int getOutput_H() {
        return this.i;
    }

    public void setOutput_H(int i) {
        this.i = i;
    }

    public int getOutput_W() {
        return this.j;
    }

    public void setOutput_W(int i) {
        this.j = i;
    }

    public int getOutput_Number() {
        return this.k;
    }

    public void setOutput_Number(int i) {
        this.k = i;
    }
}
