package a.a.a.a.a.a;

import android.content.Context;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatData.class */
class StatData {

    /* renamed from: a  reason: collision with root package name */
    private String f4a;
    private Map<String, StatModelItem> b = new HashMap();
    private Map<String, Integer> c;

    /* loaded from: easyedge-sdk.jar:a/a/a/a/a/a/StatData$StatModelItem.class */
    public static class StatModelItem {

        /* renamed from: a  reason: collision with root package name */
        private int f5a;
        private int b;
        private Map<String, Integer> c = new HashMap();

        public StatModelItem(int i, int i2) {
            this.f5a = i;
            this.b = i2;
        }

        public int b() {
            return this.f5a;
        }

        public int c() {
            return this.b;
        }

        public void a(String str, Integer num) {
            this.c.put(str, num);
        }

        public Map<String, Integer> a() {
            return this.c;
        }
    }

    public StatData(Context context, String str, String str2) {
        this.f4a = str;
    }

    public void a(int i, int i2, String str, Integer num) {
        String str2 = i + "_" + i2;
        StatModelItem statModelItem = this.b.get(str2);
        StatModelItem statModelItem2 = statModelItem;
        if (statModelItem == null) {
            statModelItem2 = new StatModelItem(i, i2);
            this.b.put(str2, statModelItem2);
        }
        statModelItem2.a(str, num);
    }

    public Map<String, Integer> c() {
        return this.c;
    }

    public void a(Map<String, Integer> map) {
        this.c = map;
    }

    public String a() {
        return this.f4a;
    }

    public Collection<StatModelItem> b() {
        return this.b.values();
    }
}
