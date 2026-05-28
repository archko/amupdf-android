package com.baidu.ai.edge.core.base;

import android.content.res.AssetManager;
import android.text.TextUtils;

import com.baidu.ai.edge.core.util.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/BaseConfig.class */
public abstract class BaseConfig implements IBaseConfig {
    public static final float DEFAULT_THRESHOLD = 0.3f;

    /* renamed from: a  reason: collision with root package name */
    protected String f8a;
    protected String[] b;
    protected int c;
    protected int d;
    protected int e;
    protected int f;
    protected float g;
    protected String h;
    protected int i;
    protected String j;
    protected boolean k;
    protected boolean l;
    protected String p;
    private long s;
    private String t;
    private String u;
    protected PreprocessConfig v;
    protected boolean m = true;
    protected int n = -1;
    private String o = "";
    private String q = null;
    private String r = Consts.AUTH_MODE_DEVICE;

    public BaseConfig() {
    }

    public BaseConfig(AssetManager assetManager, String str) throws CallException {
        String readFileIfExists;
        String readFileIfExists2;
        String readFileIfExists3;
        String readFile;
        boolean z = true;
        if (str.startsWith("file:///")) {
            str = str.substring(7);
            z = false;
        }
        String str2 = str + "/conf.json";
        String str3 = str + "/preprocess_args.json";
        String str4 = str + "/infer_cfg.json";
        String str5 = str + "/label_list.txt";
        if (z) {
            readFileIfExists = FileUtil.readAssetsFileUTF8StringIfExists(assetManager, str2);
            readFileIfExists2 = FileUtil.readAssetsFileUTF8StringIfExists(assetManager, str4);
        } else {
            readFileIfExists = FileUtil.readFileIfExists(str2);
            readFileIfExists2 = FileUtil.readFileIfExists(str4);
        }
        if (TextUtils.isEmpty(readFileIfExists) && TextUtils.isEmpty(readFileIfExists2)) {
            throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "No config file");
        }
        try {
            if (z) {
                readFileIfExists3 = FileUtil.readAssetsFileUTF8StringIfExists(assetManager, str3);
                readFile = FileUtil.readAssetFileUtf8String(assetManager, str5);
            } else {
                readFileIfExists3 = FileUtil.readFileIfExists(str3);
                readFile = FileUtil.readFile(str5);
            }
            a(readFileIfExists, readFileIfExists3, readFile, readFileIfExists2);
        } catch (CallException | IOException e) {
            throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "config read asset file error " + str, e);
        }
    }

    public static String[] getArrayStringValues(JSONArray jSONArray) throws JSONException {
        int length = jSONArray.length();
        String[] strArr = new String[length];
        for (int i = 0; i < length; i++) {
            strArr[i] = jSONArray.getString(i);
        }
        return strArr;
    }

    public static float[] getArrayFloatValues(JSONArray jSONArray) throws JSONException {
        int length = jSONArray.length();
        float[] fArr = new float[length];
        for (int i = 0; i < length; i++) {
            fArr[i] = (float) jSONArray.getDouble(i);
        }
        return fArr;
    }

    private JSONObject b(String str) throws CallException {
        try {
            JSONObject jSONObject = new JSONObject(str);
            JSONObject jSONObject2 = jSONObject.getJSONObject("model_info");
            this.f = jSONObject2.optInt("n_type", 0);
            this.g = (float) jSONObject2.optDouble("best_threshold", 0.30000001192092896d);
            this.i = jSONObject2.getInt("model_kind");
            if (jSONObject.has("block")) {
                JSONObject jSONObject3 = jSONObject.getJSONObject("block");
                if (jSONObject3.has("yolo")) {
                    this.p = jSONObject3.getJSONObject("yolo").optString("detection");
                }
            }
            if (jSONObject.has("extra")) {
                JSONObject jSONObject4 = jSONObject.getJSONObject("extra");
                if (jSONObject4.has("fluid")) {
                    JSONObject jSONObject5 = jSONObject4.getJSONObject("fluid");
                    if (jSONObject5.has("optType") && "nb".equals(jSONObject5.getString("optType"))) {
                        this.l = true;
                    }
                    if (jSONObject5.has("quantization") && "int8".equals(jSONObject5.getString("quantization"))) {
                        this.k = true;
                    }
                }
            }
            this.j = Consts.PROD_EASYEDGE_FREE;
            this.q = "no-auth";
            this.m = false;
            this.n = 0;
            return jSONObject;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CallException(Consts.EC_BASE_CONFIG_PARSE_JSON, "infer_cfg.json parse error: " + e.getMessage(), e);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v14, types: [org.json.JSONObject] */
    /* JADX WARN: Type inference failed for: r0v17, types: [java.lang.String] */
    private void a(String str, String str2, String str3, String str4) throws CallException {
        JSONObject jSONObject = null;
        JSONObject jSONObject2 = null;
        if (!TextUtils.isEmpty(str4)) {
            if (!TextUtils.isEmpty(str)) {
                jSONObject = a(str);
            }
            JSONObject js = b(str4);
            jSONObject2 = js;
            try {
                str2 = js.getString("pre_process");
            } catch (JSONException e) {
                e.printStackTrace();
                throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "No pre_process config", e);
            }
        } else if (TextUtils.isEmpty(str) && TextUtils.isEmpty(str2)) {
            throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "No conf.json and preprocess.json");
        } else {
            jSONObject = a(str);
        }
        JSONObject jSONObject3 = jSONObject;
        JSONObject jSONObject4 = jSONObject2;
        d(str2);
        c(str3);
        try {
            a(jSONObject3, jSONObject4);
        } catch (JSONException e2) {
            throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "parse JsonError ", e2);
        }
    }

    private void c(String str) {
        String[] split = str.replaceAll("\r", "").split("\n");
        if (this.i == 100 && this.v.h() == 6) {
            ArrayList arrayList = new ArrayList(Arrays.asList(split));
            arrayList.add(0, "#");
            arrayList.add(" ");
            this.b = (String[]) arrayList.toArray(new String[0]);
        } else {
            ArrayList arrayList2 = new ArrayList();
            for (String str2 : split) {
                if (Pattern.matches("\\d+:.+", str2)) {
                    arrayList2.add(str2.substring(str2.indexOf(58) + 1));
                } else {
                    arrayList2.add(str2);
                }
            }
            this.b = (String[]) arrayList2.toArray(new String[0]);
        }
        this.c = this.b.length;
    }

    private void d(String str) throws CallException {
        this.v = new PreprocessConfig(str, this.i, this.f);
    }

    public boolean isOptModel() {
        return this.l;
    }

    public boolean isEnc() {
        return this.m;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public int getMid() {
        return this.d;
    }

    public void setMid(int i) {
        this.d = i;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public int getRid() {
        return this.e;
    }

    public void setRid(int i) {
        this.e = i;
    }

    public float getRecommendedConfidence() {
        return this.g;
    }

    public void setRecommendedConfidence(int i) {
        this.g = i;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public boolean isAcceleration() {
        return this.k;
    }

    public void setAcceleration(boolean z) {
        this.k = z;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getProduct() {
        return this.j;
    }

    public void setProduct(String str) {
        this.j = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getModelFileAssetPath() {
        return this.f8a;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public void setModelFileAssetPath(String str) {
        this.f8a = str;
    }

    public int getCateNum() {
        return this.c;
    }

    public void setCateNum(int i) {
        this.c = i;
    }

    public int getNType() {
        return this.f;
    }

    public String[] getLabels() {
        return this.b;
    }

    public void setLabels(String[] strArr) {
        this.b = strArr;
    }

    public int getModelType() {
        return this.i;
    }

    public void setModelType(int i) {
        this.i = i;
    }

    public boolean isHWC() {
        return this.v.c().equals("HWC");
    }

    public boolean isRGB() {
        return this.v.d().equals("RGB");
    }

    public int getModelEncValue() {
        return this.n;
    }

    public int getTargetSize() {
        return this.v.t();
    }

    public int getMaxSize() {
        return this.v.g();
    }

    public float[] getImgMeans() {
        return this.v.f();
    }

    public float[] getScales() {
        return this.v.s();
    }

    public String getColorFormat() {
        return this.v.d();
    }

    public String getChannelOrder() {
        return this.v.c();
    }

    public int getImageWidth() {
        return this.v.q();
    }

    public int getImageHeight() {
        return this.v.p();
    }

    public PreprocessConfig getPreprocessConfig() {
        return this.v;
    }

    protected JSONObject a(String str) throws CallException {
        try {
            JSONObject jSONObject = new JSONObject(str);
            if (jSONObject.has("thresholdRec")) {
                this.g = (float) jSONObject.getDouble("thresholdRec");
            } else {
                this.g = 0.3f;
            }
            if (jSONObject.has("nType")) {
                this.f = jSONObject.getInt("nType");
            } else {
                this.f = 0;
            }
            if (jSONObject.has("extra")) {
                JSONObject jSONObject2 = jSONObject.getJSONObject("extra");
                if (jSONObject2.has("fluid")) {
                    JSONObject jSONObject3 = jSONObject2.getJSONObject("fluid");
                    if (jSONObject3.has("optType") && jSONObject3.getString("optType").equals("nb")) {
                        this.l = true;
                    }
                    if (jSONObject3.has("quantization") && jSONObject3.getString("quantization").equals("int8")) {
                        this.k = true;
                    }
                }
                this.p = jSONObject2.optString("detection", "");
            }
            if (jSONObject.has("authType")) {
                this.q = jSONObject.getString("authType");
                if ("acceleration".equals(this.q)) {
                    this.k = true;
                }
            }
            this.i = jSONObject.optInt("modelType");
            this.e = jSONObject.optInt("releaseId");
            this.d = jSONObject.optInt("modelId");
            this.j = jSONObject.optString("product");
            if ("no-auth".equals(this.q) || ((this.q == null && this.j.equals(Consts.PROD_EASYEDGE_FREE)) || this.j.equals(Consts.PROD_BML_FREE))) {
                this.m = false;
            }
            if (jSONObject.has("modelEnc")) {
                this.n = jSONObject.getInt("modelEnc");
            } else if (this.j.equals(Consts.PROD_PRO)) {
                this.n = 2;
            } else {
                this.n = 1;
            }
            this.h = jSONObject.optString("modelName", "");
            return jSONObject;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new CallException(Consts.EC_BASE_CONFIG_PARSE_JSON, " conf.json parse error " + e.getMessage(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void a(JSONObject jSONObject, JSONObject jSONObject2) throws JSONException {
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getUserDeviceId() {
        return this.o;
    }

    public void setUserDeviceId(String str) {
        this.o = str;
    }

    public String getAuthType() {
        return this.q;
    }

    public void setAuthType(String str) {
        this.q = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getAuthMode() {
        return this.r;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public long getAuthInterval() {
        return this.s;
    }

    public void setInstanceAuthMode() {
        setInstanceAuthMode(Consts.AUTH_DEF_INTERVAL);
    }

    public void setInstanceAuthMode(long j) {
        this.s = Consts.AUTH_DEF_INTERVAL;
        if (j > 5) {
            this.s = j;
        }
        this.r = Consts.AUTH_MODE_INSTANCE;
    }

    public void setAuthDomain(String str) {
        this.t = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getAuthDomain() {
        return this.t;
    }

    public void setDeviceLicenseUri(String str) {
        this.u = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getDeviceLicenseUri() {
        return this.u;
    }

    public String getExtraDetectionJson() {
        return !TextUtils.isEmpty(this.p) ? this.p : this.v.e();
    }

    public String getModelName() {
        return this.h;
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [org.json.JSONException, org.json.JSONObject] */
    public String getModelInfo() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("modelId", getMid());
            jSONObject.put("modelName", getModelName());
            jSONObject.put("modelType", getModelType());
            jSONObject.put("thresholdRec", getRecommendedConfidence());
        } catch (JSONException unused) {
            unused.printStackTrace();
        }
        return jSONObject.toString();
    }
}
