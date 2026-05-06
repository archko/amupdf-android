package com.baidu.ai.edge.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/DeviceInfo.class */
public class DeviceInfo {
    private static final byte[] b = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70};

    /* renamed from: a  reason: collision with root package name */
    private Context f24a;

    public DeviceInfo(Context context) {
        this.f24a = context;
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getSystemModel() {
        return Build.MODEL;
    }

    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    private static String a(byte[] bArr) {
        if (bArr == null || bArr.length != 6) {
            return "";
        }
        byte[] bArr2 = new byte[17];
        int i = 0;
        for (int i2 = 0; i2 <= 5; i2++) {
            int i3 = i2;
            byte b2 = bArr[i2];
            byte[] bArr3 = b;
            bArr2[i] = bArr3[(b2 & 240) >> 4];
            bArr2[i + 1] = bArr3[b2 & 15];
            if (i3 != 5) {
                bArr2[i + 2] = 58;
                i += 3;
            }
        }
        return new String(bArr2);
    }

    public static Map<String, String> getMacArray() {
        Enumeration<NetworkInterface> networkInterfaces = null;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (Exception e) {
            Log.e("tag", e.getMessage(), e);
        }
        if (networkInterfaces == null) {
            return null;
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface nextElement = networkInterfaces.nextElement();
            String a2 = null;
            try {
                a2 = a(nextElement.getHardwareAddress());
            } catch (SocketException e) {

            }
            if (!a2.isEmpty()) {
                linkedHashMap.put(nextElement.getName(), a2);
            }
        }
        return linkedHashMap;
    }

    public static String getSerial() {
        String str;
        if (Build.VERSION.SDK_INT < 26) {
            str = Build.SERIAL;
        } else {
            try {
                Class<?> cls = Class.forName("android.os.Build");
                str = (String) cls.getMethod("getSerial", new Class[0]).invoke(cls, new Object[0]);
            } catch (Exception unused) {
                str = "unknown";
            }
        }
        return str;
    }

    public static String getPingModifiedTime() {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Long.valueOf(new File("/bin/ping").lastModified()));
    }

    public String getBundleId() {
        return this.f24a.getPackageName();
    }

    public String getDeviceId() {
        String result;
        synchronized (DeviceInfo.class) {
            SharedPreferences sharedPreferences = this.f24a.getSharedPreferences("easydl-app", 0);
            String string = sharedPreferences.getString("id", "");
            if (string.isEmpty()) {
                string = UUID.randomUUID().toString();
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString("id", string);
                edit.commit();
            }
            result = string;
        }
        return result;
    }
}
