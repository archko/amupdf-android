package com.baidu.ai.edge.core.infer;

import android.content.res.AssetManager;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/infer/ArmGpuConfig.class */
public class ArmGpuConfig extends InferConfig {
    private boolean z;

    public ArmGpuConfig(AssetManager assetManager, String str) throws CallException {
        super(assetManager, str);
        if (this.m) {
            this.f8a = str + "/params.enc";
        } else {
            this.f8a = str + "/params";
        }
    }

    public void setOpenclTune(boolean z) {
        this.z = z;
    }

    public boolean isOpenclTune() {
        return this.z;
    }

    @Override // com.baidu.ai.edge.core.infer.InferConfig, com.baidu.ai.edge.core.base.IBaseConfig
    public String getSoc() {
        return Consts.SOC_ARM_GPU;
    }
}
