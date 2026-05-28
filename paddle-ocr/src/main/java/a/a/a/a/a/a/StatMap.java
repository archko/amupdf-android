package a.a.a.a.a.a;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.baidu.ai.edge.core.util.Util;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatMap.class */
public class StatMap {
    private static ConcurrentHashMap<String, StatMap> d = new ConcurrentHashMap<>();

    /* renamed from: a  reason: collision with root package name */
    private ConcurrentHashMap<String, AtomicInteger> f2a;
    private SharedPreferences b;
    private String c;

    private StatMap(Context context, String str) throws JSONException {
        this.c = str;
        this.b = context.getSharedPreferences("easyedge-app", 0);
        String string = this.b.getString("statMap-" + str, "");
        this.f2a = new ConcurrentHashMap<>();
        if (string.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : Util.jsonObjectToIntMap(new JSONObject(string)).entrySet()) {
            this.f2a.put(entry.getKey(), new AtomicInteger(entry.getValue().intValue()));
        }
    }

    public static StatMap a(Context context, String str) throws JSONException {
        StatMap statMap = new StatMap(context, str);
        StatMap putIfAbsent = d.putIfAbsent(str, statMap);
        StatMap statMap2 = putIfAbsent;
        if (putIfAbsent == null) {
            statMap2 = statMap;
        }
        return statMap2;
    }

    public void a(String str) {
        AtomicInteger atomicInteger = this.f2a.get(str);
        if (atomicInteger == null) {
            this.f2a.put(str, new AtomicInteger(1));
        } else {
            atomicInteger.incrementAndGet();
        }
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [java.lang.Throwable, java.lang.Class<a.a.a.a.a.a.StatMap>] */
    public void b(String str) {
        synchronized (StatMap.class) {
            String str2 = "statMap-" + this.c;
            SharedPreferences.Editor edit = this.b.edit();
            edit.putString(str2, str);
            edit.commit();
            Log.i("StatMap", "save " + str2 + ":" + str);
        }
    }

    public void c() throws JSONException {
        b(Util.mapToJsonObject(b()).toString());
    }

    public void a() {
        b("");
        this.f2a.clear();
    }

    public Map<String, Integer> b() {
        HashMap hashMap = new HashMap();
        for (Map.Entry<String, AtomicInteger> entry : this.f2a.entrySet()) {
            hashMap.put(entry.getKey(), Integer.valueOf(entry.getValue().get()));
        }
        return hashMap;
    }
}
