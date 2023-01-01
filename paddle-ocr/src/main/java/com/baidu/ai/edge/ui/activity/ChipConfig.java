package com.baidu.ai.edge.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class ChipConfig {
    private static final String TAG = "ChipConfig";
    private String modelName = "";
    private String version = "";
    private String soc;
    private ArrayList<String> socList = new ArrayList<>();
    private int modelType;
    private Context context;

    // 请替换为您的序列号
    public static final String SERIAL_NUM = "XXXX-XXXX-XXXX-XXXX";

    public ChipConfig(Context context) {
        this.context = context;
    }

    public boolean checkChip() {
        initConfig();
        if (socList.contains(Consts.SOC_DSP) && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = Consts.SOC_DSP;
            return true;
        }
        if (socList.contains(Consts.SOC_ADRENO_GPU) && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = Consts.SOC_ADRENO_GPU;
            return true;
        }
        if (socList.contains(Consts.SOC_NPU) && Build.HARDWARE.contains("kirin980")) {
            soc = "npu200";
            return true;
        }
        if (socList.contains(Consts.SOC_NPU_VINCI)
                && (Build.HARDWARE.contains("kirin810") || Build.HARDWARE.contains("kirin820")
                || Build.HARDWARE.contains("kirin990") || Build.HARDWARE.contains("kirin985"))) {
            soc = Consts.SOC_NPU_VINCI;
            return true;
        }
        if (socList.contains(Consts.SOC_ARM_GPU)) {
            try {
                if (InferManager.isSupportOpencl()) {
                    soc = Consts.SOC_ARM_GPU;
                    return true;
                }
            } catch (CallException e) {
                Toast.makeText(context, e.getErrorCode() + ", " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
        if (socList.contains(Consts.SOC_ARM)) {
            soc = Consts.SOC_ARM;
            return true;
        }
        return false;
    }

    public String getSoc() {
        return soc;
    }

    private void startUICameraActivity() {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra("name", modelName);
        intent.putExtra("model_type", modelType);
        intent.putExtra("serial_num", SERIAL_NUM);
        intent.putExtra("soc", soc);
    }

    /**
     * demo文件夹非必需，如果没有默认使用通用arm的配置
     */
    private void initConfig() {
        if (initConfigFromDemoConfig()) {
            Log.i(TAG, "Initialized by demo/config.json");
            return;
        }
        if (initConfigFromDemoConf()) {
            Log.i(TAG, "Initialized by demo/conf.json");
            return;
        }

        /* 从infer/读配置 */
        String confJson = FileUtil.readAssetsFileUTF8StringIfExists(context.getAssets(),
                Consts.ASSETS_DIR_ARM + "/conf.json");
        if (!TextUtils.isEmpty(confJson)) {
            try {
                JSONObject confObj = new JSONObject(confJson);
                modelName = confObj.optString("modelName", "");

                String str = confObj.optString("soc", Consts.SOC_ARM);
                String[] socs = str.split(",");
                socList.addAll(Arrays.asList(socs));

                modelType = confObj.getInt("modelType");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            confJson = FileUtil.readAssetsFileUTF8StringIfExists(context.getAssets(),
                    Consts.ASSETS_DIR_ARM + "/infer_cfg.json");
            try {
                JSONObject confObj = new JSONObject(confJson);
                socList.add(Consts.SOC_ARM);
                modelType = confObj.getJSONObject("model_info").getInt("model_kind");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "Initialized by arm#*.json");
    }

    /**
     * 原有的
     */
    private boolean initConfigFromDemoConfig() {
        String confJson = FileUtil.readAssetsFileUTF8StringIfExists(context.getAssets(), "demo/config.json");
        if (TextUtils.isEmpty(confJson)) {
            return false;
        }
        try {
            JSONObject confObj = new JSONObject(confJson);
            modelName = confObj.optString("modelName", "");

            String str = confObj.optString("soc", Consts.SOC_ARM);
            String[] socs = str.split(",");
            socList.addAll(Arrays.asList(socs));

            modelType = confObj.getInt("modelType");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 开放模型
     */
    private boolean initConfigFromDemoConf() {
        String confJson = FileUtil.readAssetsFileUTF8StringIfExists(context.getAssets(), "demo/conf.json");
        if (TextUtils.isEmpty(confJson)) {
            return false;
        }
        try {
            JSONObject confObj = new JSONObject(confJson);
            modelName = confObj.optString("modelName", "");
            socList.add(Consts.SOC_ARM);

            String inferCfgJson = FileUtil.readAssetsFileUTF8StringIfExists(context.getAssets(),
                    Consts.ASSETS_DIR_ARM + "/infer_cfg.json");
            if (TextUtils.isEmpty(inferCfgJson)) {
                return false;
            }
            JSONObject inferCfgObj = new JSONObject(inferCfgJson);
            modelType = inferCfgObj.getJSONObject("model_info").getInt("model_kind");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }
}
