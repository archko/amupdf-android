package com.baidu.ai.edge.core.detect;

import android.graphics.Rect;
import android.util.Log;
import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseManager;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.ddk.DavinciManager;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import java.util.ArrayList;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/detect/DetectPostProcess.class */
public class DetectPostProcess {

    /* renamed from: a  reason: collision with root package name */
    private int f21a;
    private int b;
    private int c;
    private int d;
    private int e;
    private BaseConfig f;
    private int g;
    private int h;
    private int i;
    private int j;
    private int k;
    private boolean l;
    private boolean m;

    public DetectPostProcess(Class<? extends BaseManager> cls, BaseConfig baseConfig, int i, int i2, int i3, int i4, int i5) {
        this.l = false;
        this.m = false;
        this.f = baseConfig;
        this.g = i;
        this.h = i2;
        this.i = i3;
        this.j = i4;
        this.k = i5;
        this.f21a = baseConfig.getNType();
        if (cls.equals(InferManager.class)) {
            this.b = 0;
            this.d = 1;
            this.c = 2;
            this.e = 6;
            this.l = true;
        } else if (cls.equals(DDKManager.class) || cls.equals(SnpeManager.class)) {
        } else {
            if (!cls.equals(DavinciManager.class)) {
                throw new RuntimeException("DetectPostProcess class not support " + cls.getSimpleName());
            }
            this.b = 1;
            this.d = 2;
            this.c = 3;
            this.e = 7;
            this.l = true;
            this.m = true;
        }
    }

    private int a(float f, int i) {
        int i2 = this.f21a;
        if (i2 == 101 || i2 == 102 || i2 == 109 || i2 == 110) {
            if (this.k == 3) {
                return Math.round(f);
            }
            f = i % 2 == 0 ? f / this.i : f / this.j;
        }
        return Math.round(i % 2 == 0 ? f * this.g : f * this.h);
    }

    public List<DetectionResultModel> a(float[] fArr, float f) {
        int i;
        ArrayList arrayList = new ArrayList();
        String[] labels = this.f.getLabels();
        int length = fArr.length;
        if (this.l) {
            int i2 = this.e;
            int i3 = (length / i2) * i2;
            i = i3;
            if (i3 != length) {
                Log.w("DetectPostProcess", "output length is " + length);
            }
        } else {
            i = length;
        }
        int i4 = 0;
        while (true) {
            int i5 = i4;
            if (i5 >= i) {
                break;
            }
            float f2 = fArr[i5 + this.d];
            if (this.m && f2 < 0.01d) {
                break;
            }
            if (f2 >= f) {
                int round = Math.round(fArr[i5 + this.b]);
                if (round < 0) {
                    Log.e("DetectPostProcess", "label index out of bound , index : " + round + " ,at :" + i5);
                } else {
                    String str = round < labels.length ? labels[round] : "UNKNOWN:" + round;
                    int i6 = i5 + this.c;
                    DetectionResultModel detectionResultModel = new DetectionResultModel(str, f2, new Rect(a(fArr[i6], 0), a(fArr[i6 + 1], 1), a(fArr[i6 + 2], 2), a(fArr[i6 + 3], 3)));
                    detectionResultModel.setLabelIndex(round);
                    arrayList.add(detectionResultModel);
                }
            }
            i4 = i5 + this.e;
        }
        return arrayList;
    }
}
