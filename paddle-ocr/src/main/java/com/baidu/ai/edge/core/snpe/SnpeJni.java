package com.baidu.ai.edge.core.snpe;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.ISDKJni;
import com.baidu.ai.edge.core.base.JniParam;

import java.io.IOException;
import java.util.ArrayList;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/snpe/SnpeJni.class */
public class SnpeJni implements ISDKJni, SnpeRuntimeInterface {

    /* renamed from: a  reason: collision with root package name */
    private static CallException f23a;

    public static boolean b() throws CallException {
        try {
            System.loadLibrary("edge-snpe");
            return true;
        } catch (Throwable th) {
            f23a = new CallException(Consts.EC_LOAD_NATIVE_SO_LIB_FAIL, "加载snpe so文件失败", th);
            throw f23a;
        }
    }

    public static ArrayList<Integer> a() {
        int availableRuntimeInt = getAvailableRuntimeInt();
        Log.i("SnpeJni", "runtime Int" + availableRuntimeInt);
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i : SnpeRuntimeInterface.RUNTIMES) {
            if ((availableRuntimeInt & (1 << i)) != 0) {
                arrayList.add(i);
            }
        }
        return arrayList;
    }

    /* JADX WARN: Type inference failed for: r0v5, types: [long, java.lang.Exception, java.lang.OutOfMemoryError] */
    public static long a(Context context, AssetManager assetManager, JniParam jniParam) throws BaseException {
        long init;
        try {
            init = init(context, assetManager, jniParam);
            return init;
        } catch (Exception unused) {
            throw BaseException.transform(unused, "");
        } catch (OutOfMemoryError unused2) {
            throw BaseException.transform(unused2);
        }
    }

    public static native float[] getPixels(Bitmap bitmap, float[] fArr, float[] fArr2, boolean z, boolean z2);

    public static native boolean setDspRuntimePath(String str);

    private static native int getAvailableRuntimeInt();

    public static native long init(Context context, AssetManager assetManager, JniParam jniParam);

    public static float[] a(long j, float[] fArr, int i) throws BaseException {
        return execute(j, fArr, i, 0.0f);
    }

    public static native float[] execute(long j, float[] fArr, int i, float f) throws BaseException;

    public static native void destory(long j);

    public static native String activate(Context context, AssetManager assetManager, JniParam jniParam) throws BaseException, IOException;

    public static native void deactivateInstance(Context context);

    @Override // com.baidu.ai.edge.core.base.ISDKJni
    public native String getStatJson(String str);
}
