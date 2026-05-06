package com.baidu.ai.edge.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/HttpUtil.class */
public class HttpUtil {

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/HttpUtil$b.class */
    public static class b implements HostnameVerifier {
        private b() {
        }

        @Override // javax.net.ssl.HostnameVerifier
        public boolean verify(String str, SSLSession sSLSession) {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/HttpUtil$c.class */
    public static class c implements X509TrustManager {
        private c() {
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        }

        @Override // javax.net.ssl.X509TrustManager
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private HttpUtil() {
    }

    public static boolean isOnline(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static String get(String str) throws IOException {
        Log.i("HttpUtil", "try to connect:" + str + " Thread id:" + Thread.currentThread().getId());
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        if (httpURLConnection instanceof HttpsURLConnection) {
            try {
                a((HttpsURLConnection) httpURLConnection);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new IOException(String.format("SSL issue: %s | %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setConnectTimeout(4000);
        httpURLConnection.setReadTimeout(20000);
        httpURLConnection.setDoOutput(false);
        return getResp(httpURLConnection.getInputStream());
    }

    public static String post(String str, String str2) throws IOException {
        return post(str, str2, new HashMap());
    }

    public static String postPlain(String str, String str2) throws IOException {
        HashMap hashMap = new HashMap();
        hashMap.put("Content-Type", "text/plain");
        return post(str, str2, hashMap);
    }

    public static String post(String str, String str2, Map<String, String> map) throws IOException {
        Log.i("HttpUtil", "try to connect:" + str + " Thread id:" + Thread.currentThread().getId());
        URL url = new URL(str);
        byte[] bytes = getBytes(str2);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        if (httpURLConnection instanceof HttpsURLConnection) {
            try {
                a((HttpsURLConnection) httpURLConnection);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new IOException(String.format("SSL issue: %s | %s", e.getClass().getSimpleName(), e.getMessage()));
            }
        }
        if (map.containsKey("Content-Type")) {
            httpURLConnection.setRequestProperty("Content-Type", map.get("Content-Type"));
        } else {
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
        }
        httpURLConnection.setRequestMethod("POST");
        int intValue = map.containsKey("ConnectTimeout") ? Integer.valueOf(map.get("ConnectTimeout")).intValue() : 4000;
        int intValue2 = map.containsKey("ReadTimeout") ? Integer.valueOf(map.get("ReadTimeout")).intValue() : 20000;
        httpURLConnection.setConnectTimeout(intValue);
        httpURLConnection.setReadTimeout(intValue2);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.getOutputStream().write(bytes);
        return getResp(httpURLConnection.getInputStream());
    }

    public static String post(String str, Bitmap bitmap, Map<String, Float> map) throws IOException, JSONException {
        Log.i("HttpUtil", "try to connect:" + str + " Thread id:" + Thread.currentThread().getId());
        URL url = new URL(str);
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("image", bitmapToBase64(bitmap));
        if (map != null) {
            for (Map.Entry<String, Float> entry : map.entrySet()) {
                jSONObject.put(entry.getKey(), entry.getValue());
            }
        }
        byte[] bytes = getBytes(jSONObject.toString());
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("Content-Type", "application/json");
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(4000);
        httpURLConnection.setReadTimeout(20000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.getOutputStream().write(bytes);
        return getResp(httpURLConnection.getInputStream());
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        IOException exception = null;
        ByteArrayOutputStream baos = null;
        String str = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    baos.flush();
                    baos.close();
                    str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                } catch (IOException e) {
                    exception = e;
                    e.printStackTrace();
                }
            }
        } finally {
            if (baos != null) {
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException unused) {
                    unused.printStackTrace();
                }
            }
        }
        return str;
    }

    public static String getResp(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer stringBuffer = new StringBuffer();
        char[] cArr = new char[512];
        while (true) {
            int read = bufferedReader.read(cArr);
            if (read == -1) {
                bufferedReader.close();
                return stringBuffer.toString();
            }
            stringBuffer.append(new String(cArr, 0, read));
        }
    }

    public static byte[] getBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("UTF-8");
    }

    private static void a(HttpsURLConnection httpsURLConnection) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sSLContext = SSLContext.getInstance("TLSv1.2");
        sSLContext.init(null, new TrustManager[]{new c()}, new SecureRandom());
        httpsURLConnection.setSSLSocketFactory(sSLContext.getSocketFactory());
        httpsURLConnection.setHostnameVerifier(new b());
    }
}
