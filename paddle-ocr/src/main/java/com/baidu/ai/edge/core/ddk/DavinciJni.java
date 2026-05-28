package com.baidu.ai.edge.core.ddk;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.ISDKJni;
import com.baidu.ai.edge.core.base.JniParam;
import java.io.IOException;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DavinciJni.class */
public class DavinciJni implements ISDKJni {

    /* renamed from: a  reason: collision with root package name */
    private static CallException f20a;

    public static void a() throws CallException {
        try {
            System.loadLibrary("hiai-500");
            System.loadLibrary("edge-davinci");
        } catch (Throwable th) {
            f20a = new CallException(Consts.EC_LOAD_NATIVE_SO_LIB_FAIL, "加载DDK-Davinci so文件失败", th);
            throw f20a;
        }
    }

    public static native String activate(Context context, AssetManager assetManager, JniParam jniParam) throws BaseException, IOException;

    public static native long loadModelSync(Context context, AssetManager assetManager, JniParam jniParam) throws BaseException;

    public static native float[] runModelSync(long j, JniParam jniParam, Bitmap bitmap) throws BaseException;

    public static native int unloadModelSync(long j);

    public static native void deactivateInstance(Context context);

    @Override // com.baidu.ai.edge.core.base.ISDKJni
    public native String getStatJson(String str);
}
