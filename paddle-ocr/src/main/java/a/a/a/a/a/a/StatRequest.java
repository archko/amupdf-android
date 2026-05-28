package a.a.a.a.a.a;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.baidu.ai.edge.core.base.IBaseConfig;
import com.baidu.ai.edge.core.base.ISDKJni;
import com.baidu.ai.edge.core.util.HttpUtil;
import com.baidu.ai.edge.core.util.Util;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatRequest.class */
public class StatRequest {
    private static String c = "https://verify.baidubce.com";

    /* renamed from: a  reason: collision with root package name */
    private ISDKJni f3a;
    private String b;

    public StatRequest(Context context, ISDKJni iSDKJni) {
        this.f3a = iSDKJni;
        this.b = context.getPackageName();
    }

    private JSONObject a(StatData statData) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        for (StatData.StatModelItem aVar : statData.b()) {
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("modelId", aVar.b());
            jSONObject2.put("releaseId", aVar.c());
            jSONObject2.put("data", Util.mapToJsonObject(aVar.a()));
            jSONArray.put(jSONObject2);
        }
        jSONObject.put("modelInvoke", jSONArray);
        jSONObject.put("sdkLaunch", Util.mapToJsonObject(statData.c()));
        return jSONObject;
    }

    public void a(StatData statData, IBaseConfig iBaseConfig, String str) throws JSONException, IOException {
        JSONObject baseInfoJson = Util.getBaseInfoJson(iBaseConfig, false, statData.a());
        baseInfoJson.put("bundleId", this.b);
        baseInfoJson.put("timestamp", new Date().getTime() / 1000);
        baseInfoJson.put("data", a(statData));
        baseInfoJson.put("version", 2);
        baseInfoJson.getJSONObject("terminalInfo").put("deviceId", str);
        String statJson = this.f3a.getStatJson(baseInfoJson.toString());
        HashMap hashMap = new HashMap();
        hashMap.put("ConnectTimeout", "4000");
        hashMap.put("ReadTimeout", "3000");
        hashMap.put("Content-Type", "text/plain");
        if (!TextUtils.isEmpty(iBaseConfig.getAuthDomain())) {
            c = iBaseConfig.getAuthDomain();
        }
        String post = HttpUtil.post(c + "/offline-auth/v2/usage/edge", statJson, hashMap);
        Log.i("StatRequest", "http result:" + post);
        if (post == null || post.isEmpty()) {
            throw new IOException("Request content not correct");
        }
        try {
            JSONObject jSONObject = new JSONObject(post);
            if (jSONObject.getBoolean("success") && jSONObject.getInt("status") == 0) {
                return;
            }
            throw new IOException("Request content is not successful");
        } catch (JSONException e) {
            throw new IOException("Request content is not json", e);
        }
    }
}
