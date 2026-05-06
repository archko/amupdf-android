package com.baidu.ai.edge.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import com.baidu.ai.edge.core.base.BaseManager;
import com.baidu.ai.edge.core.base.IBaseConfig;
import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/Util.class */
public class Util {

    /* renamed from: a  reason: collision with root package name */
    private static final FileFilter f28a = new a();

    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/Util$a.class */
    static class a implements FileFilter {
        a() {
        }

        @Override // java.io.FileFilter
        public boolean accept(File file) {
            String name = file.getName();
            if (name.startsWith("cpu")) {
                for (int i = 3; i < name.length(); i++) {
                    if (name.charAt(i) < '0' || name.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private Util() {
    }

    public static String getTodayStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    public static JSONObject getBaseInfoJson(IBaseConfig iBaseConfig) {
        return getBaseInfoJson(iBaseConfig, false, null);
    }

    public static JSONObject getBaseInfoJson(IBaseConfig iBaseConfig, boolean z, String str) {
        JSONObject result = new JSONObject();
        try {
            JSONObject terminalInfo = new JSONObject();
            terminalInfo.put("osType", "ANDROID");
            terminalInfo.put("osVersion", DeviceInfo.getAndroidVersion());
            terminalInfo.put("manufacturer", DeviceInfo.getDeviceBrand() + " | " + DeviceInfo.getSystemModel());
            if (str != null) {
                terminalInfo.put("deviceId", str);
            }
            JSONObject sdkInfo = new JSONObject();
            sdkInfo.put("sdkVersion", BaseManager.VERSION);
            sdkInfo.put("sdkType", "Android");
            if (iBaseConfig.getSoc().equals("xeye")) {
                sdkInfo.put("sdkLevel", "XEYE");
            } else if (iBaseConfig.isAcceleration()) {
                sdkInfo.put("sdkLevel", "ACCELERATION");
            } else {
                sdkInfo.put("sdkLevel", "BASIC");
            }
            sdkInfo.put("os", "ANDROID");
            sdkInfo.put("product", iBaseConfig.getProduct());
            sdkInfo.put("soc", iBaseConfig.getSoc());
            result.put("sdkInfo", sdkInfo);
            if (z) {
                JSONObject mInfo = new JSONObject();
                mInfo.put("mId", iBaseConfig.getMid());
                mInfo.put("rId", iBaseConfig.getRid());
                mInfo.put("authType", iBaseConfig.isAcceleration() ? "ACCELERATION" : "COMMON");
                result.put("mInfo", mInfo);
                terminalInfo.put("deviceIdDetail", a(iBaseConfig));
            }
            result.put("terminalInfo", terminalInfo);
            result.put("logId", UUID.randomUUID().toString());
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject a(IBaseConfig iBaseConfig) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        Map<String, String> macArray = DeviceInfo.getMacArray();
        JSONObject jSONObject2 = new JSONObject();
        for (Map.Entry<String, String> entry : macArray.entrySet()) {
            jSONObject2.put(entry.getKey(), entry.getValue());
        }
        jSONObject.put("sdk.version", BaseManager.VERSION);
        jSONObject.put("macs", jSONObject2);
        jSONObject.put("build_version_sdk_int", Build.VERSION.SDK_INT);
        jSONObject.put("java_serial", DeviceInfo.getSerial());
        jSONObject.put("build.BRAND", Build.BRAND);
        jSONObject.put("build.MODEL ", Build.MODEL);
        jSONObject.put("build.DISPLAY", Build.DISPLAY);
        jSONObject.put("build.HOST", Build.HOST);
        jSONObject.put("build.MANUFACTURER", Build.MANUFACTURER);
        jSONObject.put("build.FINGERPRINT", Build.FINGERPRINT);
        jSONObject.put("build.Time", Build.TIME);
        jSONObject.put("ping_modify_time", DeviceInfo.getPingModifiedTime());
        String replaceAll = iBaseConfig.getUserDeviceId().replaceAll("[^A-Za-z0-9\\-_]", "");
        jSONObject.put("userDeviceId", replaceAll.substring(0, Math.min(0, replaceAll.length())));
        return jSONObject;
    }

    public static String getDeviceId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("edge_sdk_uuid", 0);
        String string = sharedPreferences.getString("uuid", "");
        String str = string;
        if (TextUtils.isEmpty(string)) {
            str = UUID.randomUUID().toString();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString("uuid", str);
            edit.commit();
        }
        return str == null ? "" : str;
    }

    public static JSONObject mapToJsonObject(Map<String, ?> map) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            jSONObject.put(entry.getKey(), entry.getValue());
        }
        return jSONObject;
    }

    public static Map<String, Integer> jsonObjectToIntMap(JSONObject jSONObject) throws JSONException {
        HashMap hashMap = new HashMap();
        Iterator<String> keys = jSONObject.keys();
        while (keys.hasNext()) {
            String next = keys.next();
            hashMap.put(next, Integer.valueOf(jSONObject.getInt(next)));
        }
        return hashMap;
    }

    public static int getInferCores() {
        int a2 = a();
        if (a2 > 1) {
            return a2 / 2;
        }
        return 1;
    }

    private static int a() {
        int i;
        try {
            i = new File("/sys/devices/system/cpu/").listFiles(f28a).length;
        } catch (NullPointerException unused) {
            i = 1;
            return i;
        } catch (SecurityException unused2) {
            i = 1;
            return i;
        }
        return i;
    }
}
