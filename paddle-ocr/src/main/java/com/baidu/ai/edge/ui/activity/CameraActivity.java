package com.baidu.ai.edge.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.classify.ClassifyException;
import com.baidu.ai.edge.core.infer.InferConfig;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.ocr.OcrInterface;
import com.baidu.ai.edge.core.ocr.OcrResultModel;
import com.baidu.ai.edge.core.util.Util;
import com.baidu.ai.edge.ui.util.UiLog;
import com.baidu.ai.edge.ui.view.model.BasePolygonResultModel;
import com.baidu.ai.edge.ui.view.model.BaseResultModel;
import com.baidu.ai.edge.ui.view.model.OcrViewResultModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.AppExecutors;
import cn.archko.pdf.utils.FileUtils;
import cn.archko.pdf.utils.StreamUtils;

/**
 * Created by ruanshimin on 2018/10/31.
 */

public class CameraActivity extends AbsCameraActivity {

    private String serialNum;

    OcrInterface mOcrManager;

    private int platform = Consts.TYPE_INFER;

    private boolean isInitializing = false;

    // 模型加载状态
    private boolean modelLoadStatus = false;

    @Override
    public void onActivityCreate() {
        ChipConfig chipConfig = new ChipConfig(this);
        if (chipConfig.checkChip()) {
            choosePlatform(chipConfig.getSoc());
            start();
        } else {
            onBackPressed();
        }
    }

    private void choosePlatform(String soc) {
        switch (soc) {
            case "dsp":
                platform = Consts.TYPE_SNPE;
                break;
            case "adreno-gpu":
                platform = Consts.TYPE_SNPE_GPU;
                break;
            case "npu-vinci":
                platform = Consts.TYPE_DDK_DAVINCI;
                break;
            case "npu200":
                platform = Consts.TYPE_DDK200;
                break;
            case "arm-gpu":
                platform = Consts.TYPE_ARM_GPU;
                break;
            default:
            case "arm":
                platform = Consts.TYPE_INFER;
        }
    }

    private void start() {
        // paddleLite需要保证初始化与预测在同一线程保证速度
        AppExecutors.Companion.getInstance().diskIO().execute(() -> {
            initManager();
            if (isInitializing) {
                String path = getIntent().getStringExtra("path");
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                showResultPage(bitmap);
            }
        });
    }

    /**
     * 此处简化，建议一个mDetectManager对象在同一线程中调用
     */
    @Override
    public void onActivityDestory() {
        releaseEasyDL();
    }

    @Override
    public void onOcrBitmap(Bitmap bitmap, float confidence, ResultListener.OcrListener listener) {
        List<OcrResultModel> modelList = null;
        try {
            modelList = mOcrManager.ocr(bitmap, confidence);
            List<BasePolygonResultModel> results = new ArrayList<>();
            for (int i = 0; i < modelList.size(); i++) {
                OcrResultModel mOcrResultModel = modelList.get(i);
                OcrViewResultModel mOcrViewResultModel = new OcrViewResultModel();
                mOcrViewResultModel.setColorId(mOcrResultModel.getLabelIndex());
                mOcrViewResultModel.setIndex(i + 1);
                mOcrViewResultModel.setConfidence(mOcrResultModel.getConfidence());
                mOcrViewResultModel.setName(mOcrResultModel.getLabel());
                mOcrViewResultModel.setBounds(mOcrResultModel.getPoints());
                mOcrViewResultModel.setTextOverlay(true);
                results.add(mOcrViewResultModel);
            }
            listener.onResult(results);
        } catch (BaseException e) {
            showError(e);
            listener.onResult(null);
        }
    }

    private void showError(BaseException e) {
        showMessage(e.getErrorCode(), e.getMessage());
        Log.e("CameraActivity", e.getMessage(), e);
    }

    private void releaseEasyDL() {
        if (model == MODEL_OCR) {
            if (mOcrManager != null) {
                try {
                    mOcrManager.destroy();
                } catch (ClassifyException e) {
                    showError(e);
                } catch (BaseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isInitializing) {
            showMessage("模型未初始化");
        } else {
            super.onBackPressed();
        }
    }

    private void initManager() {
        //serialNum = getIntent().getStringExtra("serial_num");
        serialNum = ChipConfig.SERIAL_NUM;

        float threshold = BaseConfig.DEFAULT_THRESHOLD;

        UiLog.info("model type is " + model);

        if (model == MODEL_OCR) {
            InferConfig mInferConfig = null;
            try {
                mInferConfig = new InferConfig(getAssets(), "infer");
                mInferConfig.setThread(Util.getInferCores());
                threshold = mInferConfig.getRecommendedConfidence();
                mOcrManager = new InferManager(this, mInferConfig, serialNum);

                canAutoRun = true;
                isInitializing = true;
            } catch (BaseException e) {
                showError(e);
            }
        }

        setConfidence(threshold);
    }

    public void save(List<BaseResultModel> models) {
        String pos = getIntent().getStringExtra("pos");

        StringBuilder sb = new StringBuilder();
        for (BaseResultModel baseResultModel : models) {
            sb.append(baseResultModel.getName()).append("\n");
        }
        File dir = FileUtils.getStorageDir("amupdf");
        if (dir != null && dir.exists()) {
            String filePath = dir.getAbsolutePath() + File.separator + pos + ".txt";
            try {
                StreamUtils.copyStringToFile(sb.toString(), filePath);
                Toast.makeText(this, "保存成功:" + filePath, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "保存失败:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
