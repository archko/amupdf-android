package com.baidu.ai.edge.core.ddk;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.ISDKJni;
import com.baidu.ai.edge.core.base.JniParam;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import java.io.IOException;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DDKJni.class */
public class DDKJni implements ISDKJni {

    /* renamed from: a  reason: collision with root package name */
    private static CallException f18a;

    public static CallException a() {
        return f18a;
    }

    public static boolean b() throws CallException {
        try {
            System.loadLibrary("edge-ddk");
            return true;
        } catch (Throwable th) {
            f18a = new CallException(Consts.EC_LOAD_NATIVE_SO_LIB_FAIL, "加载DDK so文件失败", th);
            throw f18a;
        }
    }

    public static native float[] getPixels(Bitmap bitmap, float[] fArr, float[] fArr2, boolean z, boolean z2);

    public static native int loadMixModelSync(Context context, AssetManager assetManager, JniParam jniParam);

    public static native int unloadMixModelSync();

    public static native List<ClassificationResultModel> runMixModelSync(DDKModelInfo dDKModelInfo, float[] fArr) throws BaseException;

    public static native float[] runMixModelDetectSync(DDKModelInfo dDKModelInfo, float[] fArr) throws BaseException;

    public static native String activate(Context context, AssetManager assetManager, JniParam jniParam) throws BaseException, IOException;

    public static native void deactivateInstance(Context context);

    @Override // com.baidu.ai.edge.core.base.ISDKJni
    public native String getStatJson(String str);
}
