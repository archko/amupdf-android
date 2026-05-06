package a.a.a.a.a.a;

import android.content.Context;
import android.util.Log;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.ISDKJni;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatManager.class */
public class StatManager {
    private static AtomicBoolean k = new AtomicBoolean(false);
    private static AtomicBoolean l = new AtomicBoolean(false);
    private static volatile long m = 0;
    private static volatile long n = 0;

    /* renamed from: a  reason: collision with root package name */
    private SimpleDateFormat f0a = new SimpleDateFormat("yyyy-MM-dd");
    private volatile StatMap b;
    private volatile StatMap c;
    private String d;
    private int e;
    private int f;
    private Context g;
    private String h;
    private ISDKJni i;
    private BaseConfig j;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatManager$StatRunner.class */
    public class StatRunner implements Runnable {
        private StatRunner() {
        }

        private StatData a() {
            StatData statData = new StatData(StatManager.this.g, StatManager.this.d, StatManager.this.h);
            statData.a(StatManager.this.b.b());
            for (Map.Entry<String, Integer> entry : StatManager.this.c.b().entrySet()) {
                String key = entry.getKey();
                String[] split = key.split("_");
                if (split.length < 2) {
                    throw new RuntimeException("internal stat key error:" + key);
                }
                int parseInt = Integer.parseInt(split[0]);
                int parseInt2 = Integer.parseInt(split[1]);
                if (parseInt < 0) {
                    throw new RuntimeException("modelId is negative:" + parseInt);
                }
                if (parseInt2 < 0) {
                    throw new RuntimeException("modelId is negative:" + parseInt);
                }
                statData.a(parseInt, parseInt2, split[2], entry.getValue());
            }
            return statData;
        }

        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [a.a.a.a.a.a.StatManager$StatRunner] */
        /* JADX WARN: Type inference failed for: r0v15 */
        /* JADX WARN: Type inference failed for: r0v2, types: [java.lang.Throwable] */
        /* JADX WARN: Type inference failed for: r0v20 */
        /* JADX WARN: Type inference failed for: r0v21 */
        /* JADX WARN: Type inference failed for: r1v9, types: [java.lang.Exception] */
        /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.Throwable] */
        /* JADX WARN: Type inference failed for: r9v0 */
        /* JADX WARN: Type inference failed for: r9v4 */
        @Override // java.lang.Runnable
        public void run() {
            Exception e = null;
            try {
                try {
                    new StatRequest(StatManager.this.g, StatManager.this.i).a(a(), StatManager.this.j, StatManager.this.d);
                    StatManager.this.c.a();
                    StatManager.this.b.a();
                    StatManager.m = System.currentTimeMillis();
                } catch (IOException e2) {
                    e = e2;
                } catch (JSONException e3) {
                    e = e3;
                }
                if (e != null) {
                    StatManager.this.b();
                    Log.e("StatManager", "Stat connection error :" + e.getClass().getSimpleName() + ":" + e.getMessage(), e);
                }
            } finally {
                StatManager.k.set(false);
            }
        }
    }

    public StatManager(Context context, ISDKJni iSDKJni, BaseConfig baseConfig, String str) {
        if (str == null || str.isEmpty()) {
            throw new RuntimeException("deviceId is empty");
        }
        this.j = baseConfig;
        this.g = context;
        this.i = iSDKJni;
        this.d = str;
        this.e = baseConfig.getMid();
        this.f = baseConfig.getRid();
        this.h = baseConfig.getProduct();
        try {
            this.b = StatMap.a(context, "start");
            this.c = StatMap.a(context, Consts.ASSETS_DIR_ARM);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String e() {
        return this.f0a.format(new Date());
    }

    private void f() {
        n = System.currentTimeMillis();
        if (k.getAndSet(true)) {
            Log.e("StatManager", "StatRequest is running");
        } else {
            new Thread(new StatRunner()).start();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [boolean] */
    /* JADX WARN: Type inference failed for: r0v4, types: [a.a.a.a.a.a.StatMap] */
    public void c() {
        boolean andSet = l.getAndSet(true);
        this.b.a(e());
        if (!andSet) {
            f();
            return;
        }
        try {
            this.b.c();
        } catch (JSONException unused) {
            unused.printStackTrace();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v11, types: [int] */
    /* JADX WARN: Type inference failed for: r0v15, types: [long] */
    public void a() {
        this.c.a(this.e + "_" + this.f + "_" + e());
        long currentTimeMillis = System.currentTimeMillis();
        if (n == 0 || currentTimeMillis < n || currentTimeMillis > n + 86400000) {
            f();
            return;
        }
        if (currentTimeMillis > m + 30000) {
            try {
                this.c.c();
                m = System.currentTimeMillis();
            } catch (JSONException unused) {
                unused.printStackTrace();
            }
        }
    }

    public void b() {
        try {
            this.c.c();
            this.b.c();
            m = System.currentTimeMillis();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("StatManager", "save error", e);
        }
    }
}
