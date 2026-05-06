package com.baidu.ai.edge.core.ddk;

import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DDKDaVinciConfig.class */
public class DDKDaVinciConfig extends BaseConfig {
    private boolean w;

    public DDKDaVinciConfig(AssetManager assetManager, String str) throws CallException {
        super(assetManager, str);
        this.w = true;
        if (this.m) {
            this.f8a = str + "/params.enc";
        } else {
            this.f8a = str + "/params";
        }
    }

    public boolean isAutoCheckNpu() {
        return this.w;
    }

    public void setAutoCheckNpu(boolean z) {
        this.w = z;
    }

    public boolean isSupportDavinciNpu() throws CallException {
        if (this.w) {
            String lowerCase = Build.HARDWARE.toLowerCase();
            if (lowerCase.contains("kirin810") || lowerCase.contains("kirin990") || lowerCase.contains("kirin820") || lowerCase.contains("kirin985")) {
                return true;
            }
            Log.e("DDKDaVinciConfig", "Your device does NOT support Davinci:" + lowerCase);
            throw new CallException(Consts.EC_CHECK_NPU_FAIL, "Your device does NOT support Davinci: " + lowerCase);
        }
        return true;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseConfig
    public String getSoc() {
        return Consts.SOC_NPU_VINCI;
    }
}
