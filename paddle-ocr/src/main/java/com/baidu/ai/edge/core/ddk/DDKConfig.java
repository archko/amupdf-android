package com.baidu.ai.edge.core.ddk;

import android.content.res.AssetManager;
import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.util.FileUtil;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DDKConfig.class */
public class DDKConfig extends BaseConfig {
    public static final String SDK_CONFIG_PATH = "sdk-config.json";

    public DDKConfig(AssetManager assetManager, String str) throws CallException {
        super(assetManager, str);
        try {
            JSONArray jSONArray = new JSONArray(str.startsWith("file:///") ? FileUtil.readFile(str.substring(7) + "/" + SDK_CONFIG_PATH) : FileUtil.readAssetFileUtf8String(assetManager, str + "/" + SDK_CONFIG_PATH));
            for (int i = 0; i < jSONArray.length(); i++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject.has("id") && jSONObject.getString("id").equals("ddk200")) {
                    this.f8a = str + "/" + jSONObject.getString("params") + (isEnc() ? ".enc" : "");
                }
            }
        } catch (IOException e) {
            throw new CallException(Consts.EC_BASE_CONFIG_READ_ASSETS_FILE, "sdk-config read asset file error " + str, e);
        } catch (JSONException e2) {
            e2.printStackTrace();
            throw new CallException(Consts.EC_BASE_CONFIG_PARSE_JSON, " sdk-config parse json error ", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.baidu.ai.edge.core.base.BaseConfig
    public void a(JSONObject jSONObject, JSONObject jSONObject2) throws JSONException {
        super.a(jSONObject, jSONObject2);
        this.v.a("CHW");
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getSoc() {
        return Consts.SOC_NPU;
    }
}
