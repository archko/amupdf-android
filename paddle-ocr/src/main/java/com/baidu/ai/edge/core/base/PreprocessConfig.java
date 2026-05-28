package com.baidu.ai.edge.core.base;

import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/PreprocessConfig.class */
public class PreprocessConfig {

    /* renamed from: a  reason: collision with root package name */
    protected int f15a;
    protected int b;
    protected float[] c;
    protected float[] d;
    protected String e;
    protected String f;
    protected String g;
    protected boolean h;
    protected String i;
    protected int j;
    protected int k;
    private int l = -1;
    private final boolean m;
    private boolean n;
    private int o;
    private int p;
    private int q;
    private int r;
    private int s;
    private boolean t;
    private String u;
    private int v;
    private int w;
    private int[] x;

    public PreprocessConfig(String str, int i, int i2) throws CallException {
        PreprocessConfig preprocessConfig;
        int i3;
        PreprocessConfig preprocessConfig2;
        int i4;
        this.g = "keep_size";
        this.i = "";
        this.o = 320;
        this.p = 32;
        this.q = 1;
        this.u = "padding_align32";
        this.v = 0;
        this.w = 0;
        this.x = new int[]{114, 114, 114};
        try {
            JSONObject jSONObject = new JSONObject(str);
            if (i == 100) {
                this.k = jSONObject.getInt("max_size");
                JSONArray optJSONArray = jSONObject.optJSONArray("ocr_rec_resize");
                if (optJSONArray != null) {
                    this.o = optJSONArray.getInt(0);
                    this.p = optJSONArray.getInt(1);
                }
                this.q = jSONObject.optInt("ocr_rec_batch_num", this.q);
            } else {
                if (jSONObject.has("resize")) {
                    JSONArray jSONArray = jSONObject.getJSONArray("resize");
                    this.f15a = jSONArray.optInt(0);
                    this.b = jSONArray.optInt(1);
                }
                this.g = jSONObject.optString("rescale_mode", this.g);
                if (i2 == 102) {
                    this.g = "keep_ratio";
                }
                if (this.g.equals("keep_ratio")) {
                    if (jSONObject.has("target_size")) {
                        preprocessConfig = this;
                        i3 = jSONObject.getInt("target_size");
                    } else {
                        preprocessConfig = this;
                        i3 = preprocessConfig.f15a;
                    }
                    preprocessConfig.j = i3;
                    if (jSONObject.has("max_size")) {
                        preprocessConfig2 = this;
                        i4 = jSONObject.getInt("max_size");
                    } else {
                        preprocessConfig2 = this;
                        i4 = preprocessConfig2.b;
                    }
                    preprocessConfig2.k = i4;
                } else if (this.g.equals("warp_affine") && jSONObject.has("warp_affine_keep_res")) {
                    this.h = jSONObject.getBoolean("warp_affine_keep_res");
                }
            }
            if (i2 == 102 || i2 == 2010) {
                this.t = true;
            }
            if (jSONObject.has("padding_mode")) {
                this.u = jSONObject.getString("padding_mode");
                if (this.u.equals("padding_align32")) {
                    this.t = true;
                } else if (this.u.equals("padding_fill_size")) {
                    this.t = true;
                    if (jSONObject.has("padding_fill_size")) {
                        JSONArray jSONArray2 = jSONObject.getJSONArray("padding_fill_size");
                        this.v = jSONArray2.optInt(0);
                        this.w = jSONArray2.optInt(1);
                    }
                    if (jSONObject.has("padding_fill_value")) {
                        JSONArray jSONArray3 = jSONObject.getJSONArray("padding_fill_value");
                        if (jSONArray3.length() == 1) {
                            int optInt = jSONArray3.optInt(0);
                            this.x = new int[]{optInt, optInt, optInt};
                        } else if (jSONArray3.length() == 3) {
                            for (int i5 = 0; i5 < 3; i5++) {
                                this.x[i5] = jSONArray3.optInt(i5);
                            }
                        } else {
                            Log.e("PreprocessConfig", "Padding fill value dims must be 1 or 3.");
                        }
                    }
                }
            }
            JSONArray optJSONArray2 = jSONObject.optJSONArray("img_mean");
            this.c = BaseConfig.getArrayFloatValues(optJSONArray2 == null ? jSONObject.getJSONArray("mean") : optJSONArray2);
            try {
                float f = (float) jSONObject.getDouble("scale");
                this.d = new float[]{f, f, f};
            } catch (JSONException unused) {
                this.d = BaseConfig.getArrayFloatValues(jSONObject.getJSONArray("scale"));
            }
            this.n = jSONObject.optBoolean("skip_norm", false);
            if (this.n) {
                this.d = new float[]{1.0f, 1.0f, 1.0f};
                this.c = new float[]{0.0f, 0.0f, 0.0f};
                Log.i("PreprocessConfig", "skip_norm");
            }
            this.e = jSONObject.optString("colorFormat");
            if (TextUtils.isEmpty(this.e)) {
                this.e = jSONObject.getString("color_format");
            }
            this.f = jSONObject.optString("channelOrder");
            if (TextUtils.isEmpty(this.f)) {
                this.f = jSONObject.optString("channel_order", "");
            }
            if (jSONObject.has("extra")) {
                this.i = jSONObject.getJSONObject("extra").optString("detection", "");
            }
            this.m = jSONObject.optBoolean("letterbox");
            if (jSONObject.has("center_crop_size")) {
                JSONArray jSONArray4 = jSONObject.getJSONArray("center_crop_size");
                this.r = jSONArray4.getInt(0);
                this.s = jSONArray4.getInt(1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CallException(Consts.EC_BASE_CONFIG_PARSE_JSON, " preprocess_args parse json error ", e);
        }
    }

    public int q() {
        return this.f15a;
    }

    public int p() {
        return this.b;
    }

    public float[] f() {
        return this.c;
    }

    public float[] s() {
        return this.d;
    }

    public String d() {
        return this.e;
    }

    public String r() {
        return this.g;
    }

    public int t() {
        return this.j;
    }

    public int g() {
        return this.k;
    }

    public String c() {
        return this.f;
    }

    public void a(String str) {
        this.f = str;
    }

    public int o() {
        return this.l;
    }

    public void a(int i) {
        this.l = i;
    }

    public String e() {
        return this.i;
    }

    public boolean u() {
        return this.m;
    }

    public boolean w() {
        return this.n;
    }

    public int j() {
        return this.o;
    }

    public int i() {
        return this.p;
    }

    public int h() {
        return this.q;
    }

    public int b() {
        return this.r;
    }

    public int a() {
        return this.s;
    }

    public boolean x() {
        return this.h;
    }

    public boolean v() {
        return this.t;
    }

    public String m() {
        return this.u;
    }

    public int l() {
        return this.v;
    }

    public int k() {
        return this.w;
    }

    public int[] n() {
        return this.x;
    }
}
