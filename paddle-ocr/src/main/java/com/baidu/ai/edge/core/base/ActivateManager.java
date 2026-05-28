package com.baidu.ai.edge.core.base;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.baidu.ai.edge.core.ddk.DDKJni;
import com.baidu.ai.edge.core.ddk.DavinciJni;
import com.baidu.ai.edge.core.infer.InferLiteJni;
import com.baidu.ai.edge.core.snpe.SnpeJni;
import com.baidu.ai.edge.core.util.HttpUtil;
import com.baidu.ai.edge.core.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/ActivateManager.class */
public final class ActivateManager {
    public static final int JNILIB_TYPE_ARM = 100;
    public static final int JNILIB_TYPE_SNPE = 101;
    public static final int JNILIB_TYPE_DDK = 102;
    public static final int JNILIB_TYPE_XEYE = 103;
    public static final int JNILIB_TYPE_DAVINCI = 104;
    private static final String URL_DEVICE_ACTIVATE = "/offline-auth/v2/key/activate/edge";
    private static final String URL_GENERATE_KEY = "/instance-auth/v1/key/generate";
    private static final String URL_INSTANCE_ACTIVATE = "/instance-auth/v1/key/activate/edge";
    private static final String URL_INSTANCE_DEACTIVATE = "/instance-auth/v1/key/deactivate/edge";
    private static String domain = "https://verify.baidubce.com";
    private static String deviceActivateUri = "/offline-auth/v2/key/activate/edge";
    private static final String TAG = "ActivateManager";
    private static volatile ScheduledThreadPoolExecutor instanceActivateExecutor;
    private Context context;
    private IBaseConfig config;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/ActivateManager$a.class */
    public class a implements Runnable {
        a(ActivateManager activateManager) {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (ActivateManager.instanceActivateExecutor != null) {
                ActivateManager.instanceActivateExecutor.shutdownNow();
                ScheduledThreadPoolExecutor unused = ActivateManager.instanceActivateExecutor = null;
                Log.i(ActivateManager.TAG, "executor terminated");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/ActivateManager$b.class */
    public static class b implements Callable<String> {

        /* renamed from: a  reason: collision with root package name */
        final /* synthetic */ String f6a;
        final /* synthetic */ String b;

        b(String str, String str2) {
            this.f6a = str;
            this.b = str2;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.concurrent.Callable
        public String call() throws Exception {
            try {
                return HttpUtil.postPlain(this.f6a, this.b);
            } catch (Exception e) {
                Log.e(ActivateManager.TAG, e.getMessage());
                throw new IOException("NETWORK STATUS IS CHECKED, NO NETWORK", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/ActivateManager$c.class */
    public class c implements Runnable {

        /* renamed from: a  reason: collision with root package name */
        private final String f7a;
        private final int b;
        private final boolean c;

        public c(String str, int i, boolean z) {
            this.f7a = str;
            this.b = i;
            this.c = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                ActivateManager.this.activateInternal(this.f7a, this.b, this.c);
            } catch (Exception e) {
                Log.e(ActivateManager.TAG, "Scheduled activation failed: " + e.getClass() + "|" + e.getMessage());
            }
        }
    }

    public ActivateManager(Context context, IBaseConfig iBaseConfig) {
        this.context = context.getApplicationContext();
        this.config = iBaseConfig;
        if (!TextUtils.isEmpty(iBaseConfig.getAuthDomain())) {
            domain = iBaseConfig.getAuthDomain();
        }
        if (!TextUtils.isEmpty(iBaseConfig.getDeviceLicenseUri())) {
            deviceActivateUri = iBaseConfig.getDeviceLicenseUri();
        }
        Log.i(TAG, "domain: " + domain + ", deviceLicenseUri: " + deviceActivateUri);
    }

    static byte[] generateKeyByHTTP(Context context, String str) throws BaseException, IOException {
        return requestByHttp(context, domain + URL_GENERATE_KEY, str);
    }

    static byte[] instanceActivateByHttp(Context context, String str, String str2) throws BaseException, IOException {
        return requestByHttp(context, domain + URL_INSTANCE_ACTIVATE + "?ak=" + str2, str);
    }

    static byte[] deactivateInstanceByHttp(Context context, String str, String str2) throws BaseException, IOException {
        return requestByHttp(context, domain + URL_INSTANCE_DEACTIVATE + "?ak=" + str2, str);
    }

    static byte[] deviceActivateByHttp(Context context, String str) throws BaseException, IOException {
        return requestByHttp(context, domain + deviceActivateUri, str);
    }

    private static byte[] requestByHttp(Context context, String str, String str2) throws BaseException, IOException {
        try {
            Log.i(TAG, "request body: " + str + " | " + str2);
            FutureTask futureTask = new FutureTask(new b(str, str2));
            new Thread(futureTask, "task").start();
            try {
                String str3 = (String) futureTask.get();
                if (HttpUtil.isOnline(context)) {
                    if (str3 == null || str3.isEmpty()) {
                        throw new BaseException(Consts.EC_BASE_INTERNAL_ERROR, "server return empty result");
                    }
                    JSONObject jSONObject = new JSONObject(str3);
                    if (jSONObject.getInt("status") == 0) {
                        Log.i(TAG, "Server activation success");
                        return Base64.decode(jSONObject.getString("result"), 2);
                    }
                    String string = jSONObject.getString("message");
                    Log.e(TAG, "Response " + jSONObject.getInt("status") + ": " + string);
                    throw new BaseException(Consts.EC_BASE_INTERNAL_ERROR, "server activation failed: " + string);
                }
                throw new IOException("NETWORK STATUS IS CHECKED, NO NETWORK");
            } catch (InterruptedException unused) {
                throw new IOException("NETWORK STATUS IS CHECKED, NO NETWORK");
            } catch (ExecutionException unused2) {
                throw new IOException("NETWORK STATUS IS CHECKED, NO NETWORK");
            }
        } catch (JSONException e) {
            throw new BaseException(Consts.EC_BASE_INTERNAL_ERROR, e.getMessage(), e);
        } catch (Throwable th) {
            throw new BaseException(Consts.EC_BASE_INTERNAL_ERROR, th.getMessage(), th);
        }
    }

    /* JADX WARN: Type inference failed for: r0v1, types: [org.json.JSONException, org.json.JSONObject] */
    private JSONObject mergeActiveJSON(IBaseConfig iBaseConfig, String str) {
        JSONObject baseInfoJson = Util.getBaseInfoJson(iBaseConfig, true, null);
        if (baseInfoJson != null) {
            try {
                baseInfoJson.put("authMode", iBaseConfig.getAuthMode());
                baseInfoJson.put("serialKey", str);
            } catch (JSONException unused) {
                unused.printStackTrace();
            }
        }
        return baseInfoJson;
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [org.json.JSONException, org.json.JSONObject] */
    private JSONObject mergeKeyJSON(String str) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("serialKey", str);
        } catch (JSONException unused) {
            Log.e(TAG, unused.getMessage());
        }
        return jSONObject;
    }

    static String getBase64(byte[] bArr) {
        return Base64.encodeToString(bArr, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String activateInternal(String str, int i, boolean z) throws IOException, CallException, BaseException {
        AssetManager assets = this.context.getAssets();
        JSONObject mergeActiveJSON = mergeActiveJSON(this.config, str);
        JniParam fillCommonAuthParam = fillCommonAuthParam(str);
        try {
            fillCommonAuthParam.put("isSkipDecrypt", Boolean.valueOf(z));
            mergeActiveJSON.put("isSkipDecrypt", z);
            fillCommonAuthParam.put("activate_json_body", mergeActiveJSON.toString());
            fillCommonAuthParam.put("authMode", this.config.getAuthMode());
            if (this.config.getAuthMode().equals(Consts.AUTH_MODE_INSTANCE)) {
                fillCommonAuthParam.put("key_json_body", mergeKeyJSON(str).toString());
            }
            fillCommonAuthParam.put("deviceActivateURI", deviceActivateUri);
            String str2 = null;
            switch (i) {
                case 100:
                    str2 = InferLiteJni.activate(this.context, assets, fillCommonAuthParam);
                    break;
                case JNILIB_TYPE_SNPE /* 101 */:
                    str2 = SnpeJni.activate(this.context, assets, fillCommonAuthParam);
                    break;
                case 102:
                    str2 = DDKJni.activate(this.context, assets, fillCommonAuthParam);
                    break;
                case JNILIB_TYPE_DAVINCI /* 104 */:
                    str2 = DavinciJni.activate(this.context, assets, fillCommonAuthParam);
                    break;
            }
            String str3 = str2;
            Log.i(TAG, "Activation succeed: " + this.config.getAuthMode());
            return str3;
        } catch (JSONException e) {
            Log.e(TAG, "json error:" + e.getMessage(), e);
            throw new CallException(Consts.EC_BASE_INTERNAL_ERROR, "json error:" + e.getMessage(), e);
        }
    }

    private void scheduleInstanceActivation(String str, int i, boolean z) throws BaseException {
        instanceActivateExecutor = new ScheduledThreadPoolExecutor(1);
        instanceActivateExecutor.scheduleAtFixedRate(new c(str, i, z), this.config.getAuthInterval(), this.config.getAuthInterval(), TimeUnit.SECONDS);
    }

    public void terminate() {
        new Thread(new a(this)).start();
    }

    public JniParam fillCommonAuthParam(String str) {
        JniParam jniParam = new JniParam();
        if (str == null) {
            str = "";
        }
        jniParam.put("serial", str);
        jniParam.put("serialNo", str);
        String[] strArr = {this.context.getFilesDir() + "/.baidu/easyedge", this.context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/.baidu/easyedge", Environment.getExternalStorageDirectory() + "/.baidu/easyedge"};
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            File file = new File(strArr[i] + "/licenses");
            if (!file.exists()) {
                if (file.mkdirs()) {
                    Log.i(TAG, "mkdir success :" + file.getAbsolutePath());
                } else {
                    Log.e(TAG, "mkdir error :" + file.getAbsolutePath());
                }
            }
        }
        jniParam.put("isAcceleration", Boolean.valueOf(this.config.isAcceleration()));
        jniParam.put("fileDir", strArr[0]);
        if (strArr.length > 1) {
            jniParam.put("externalFileDir", strArr[1]);
            jniParam.put("externalStorageDir", strArr[2]);
        }
        jniParam.put("authRequireSDCard", (Object) true);
        return jniParam;
    }

    public String activate(String str, int i, boolean z) throws IOException, CallException, BaseException {
        String activateInternal = activateInternal(str, i, z);
        if (this.config.getAuthMode().equals(Consts.AUTH_MODE_INSTANCE)) {
            scheduleInstanceActivation(str, i, z);
        }
        return activateInternal;
    }
}
