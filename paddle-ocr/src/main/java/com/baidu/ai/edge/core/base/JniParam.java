package com.baidu.ai.edge.core.base;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/JniParam.class */
public class JniParam {

    /* renamed from: a  reason: collision with root package name */
    private boolean f12a = false;
    private Map<String, Object> b = new HashMap();

    private void a(String str) {
        if (this.f12a) {
            Log.i("JniParam", "Try to read key:  " + str);
        }
    }

    public void put(String str, Object obj) {
        this.b.put(str, obj);
    }

    public void put(String str, long j) {
        this.b.put(str, Long.valueOf(j));
    }

    public int getInt(String str) {
        a(str);
        Object obj = this.b.get(str);
        if (obj == null) {
            Log.e("JniParams", "value is null : " + str);
            return 0;
        }
        return Integer.valueOf(obj.toString()).intValue();
    }

    public boolean getBool(String str) {
        a(str);
        if (this.b.containsKey(str)) {
            return ((Boolean) this.b.get(str)).booleanValue();
        }
        return false;
    }

    public long getLong(String str) {
        a(str);
        return ((Long) this.b.get(str)).longValue();
    }

    public float getFloat(String str) {
        a(str);
        return ((Float) this.b.get(str)).floatValue();
    }

    public double getDouble(String str) {
        a(str);
        return ((Double) this.b.get(str)).doubleValue();
    }

    public String getString(String str) {
        a(str);
        return (String) this.b.get(str);
    }

    public boolean isNull(String str) {
        a(str);
        return this.b.get(str) == null;
    }

    public Object getObject(String str) {
        a(str);
        return this.b.get(str);
    }

    public float[] getFloatArr(String str) {
        a(str);
        return (float[]) this.b.get(str);
    }

    public int[] getIntArr(String str) {
        a(str);
        return (int[]) this.b.get(str);
    }

    public boolean containsKey(String str) {
        a(str);
        return this.b.containsKey(str);
    }
}
