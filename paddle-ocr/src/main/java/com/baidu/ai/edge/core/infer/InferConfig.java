package com.baidu.ai.edge.core.infer;

import android.content.res.AssetManager;
import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.util.Util;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/infer/InferConfig.class */
public class InferConfig extends BaseConfig {
    private String w;
    private String x;
    private int y;

    public InferConfig(AssetManager assetManager, String str) throws CallException {
        super(assetManager, str);
        this.y = 0;
        if (this.i == 100) {
            this.f8a = str + "/model";
            this.x = str + "/params";
            this.l = true;
        } else if (this.m) {
            if (this.l) {
                this.f8a = str + "/params.enc";
                return;
            }
            this.f8a = str + "/model.enc";
            this.w = str + "/params.enc";
        } else if (this.l) {
            this.f8a = str + "/params";
        } else {
            this.f8a = str + "/model";
            this.w = str + "/params";
        }
    }

    public int getThread() {
        if (this.y == 0) {
            this.y = Util.getInferCores();
        }
        return this.y;
    }

    public void setThread(int i) {
        this.y = i;
    }

    public String getParamFileAssetPath() {
        return this.w;
    }

    public void setParamFileAssetPath(String str) {
        this.w = str;
    }

    public String getExtraModelFilePath() {
        return this.x;
    }

    public void setExtraModelFilePath(String str) {
        this.x = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getSoc() {
        return Consts.SOC_ARM;
    }
}
