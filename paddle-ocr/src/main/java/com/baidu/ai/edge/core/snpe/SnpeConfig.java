package com.baidu.ai.edge.core.snpe;

import android.content.res.AssetManager;
import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/snpe/SnpeConfig.class */
public class SnpeConfig extends BaseConfig implements ISnpeConfig, SnpeRuntimeInterface {
    private boolean w;
    protected int[] x;

    public SnpeConfig(AssetManager assetManager, String str) throws CallException {
        super(assetManager, str);
        if (this.m) {
            this.f8a = str + "/params.enc";
        } else {
            this.f8a = str + "/params";
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.baidu.ai.edge.core.base.BaseConfig
    public void a(JSONObject jSONObject, JSONObject jSONObject2) throws JSONException {
        int length;
        super.a(jSONObject, jSONObject2);
        if (this.x == null) {
            this.x = new int[]{2, 1, 3, 0};
        }
        this.w = jSONObject.optBoolean("autocheck_qcom", true);
        JSONArray optJSONArray = jSONObject.optJSONArray("snpe_runtimes_order");
        if (optJSONArray != null && (length = optJSONArray.length()) > 0) {
            this.x = new int[length];
            for (int i = 0; i < length; i++) {
                this.x[i] = optJSONArray.getInt(i);
            }
        }
        this.v.a("HWC");
    }

    @Override // com.baidu.ai.edge.core.snpe.ISnpeConfig
    public boolean isAutocheckQcom() {
        return this.w;
    }

    public void setAutocheckQcom(boolean z) {
        this.w = z;
    }

    @Override // com.baidu.ai.edge.core.snpe.ISnpeConfig
    public int[] getSnpeRuntimesOrder() {
        return (int[]) this.x.clone();
    }

    public void setSnpeRuntimesOrder(int[] iArr) {
        this.x = (int[]) iArr.clone();
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getSoc() {
        return Consts.SOC_DSP;
    }
}
