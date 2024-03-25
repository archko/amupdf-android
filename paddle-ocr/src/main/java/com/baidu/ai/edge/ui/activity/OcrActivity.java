package com.baidu.ai.edge.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
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

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.AppExecutors;
import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.utils.FileUtils;
import cn.archko.pdf.core.utils.StreamUtils;

/**
 * Created by ruanshimin on 2018/10/31.
 */

public class OcrActivity extends AbsOcrActivity {

    public static final String INTENT_PATH = "path";
    public static final String INTENT_NAME = "name";
    private ClipboardManager clipboardManager;

    public static void start(Context context, Bitmap bitmap, String path, String name) {
        Intent intent = new Intent(context, OcrActivity.class);
        intent.putExtra(INTENT_PATH, path);
        if (null != bitmap) {
            String key = String.valueOf(System.currentTimeMillis());
            BitmapCache.getInstance().addBitmap(key, bitmap);
            intent.putExtra("key", key);
        }
        intent.putExtra("name", name);
        context.startActivity(intent);
    }

    private String serialNum;

    OcrInterface mOcrManager;

    private int platform = Consts.TYPE_INFER;

    private boolean isInitializing = false;

    // 模型加载状态
    private boolean modelLoadStatus = false;
    private String mPath;
    private Bitmap bitmap;
    private String name = "ocr";

    @Override
    public void onActivityCreate() {
        parseIntent();
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
                if (bitmap == null && !TextUtils.isEmpty(mPath)) {
                    bitmap = BitmapFactory.decodeFile(mPath);
                }
                showResultPage(bitmap);
            }
        });
    }

    private void parseIntent() {
        Intent intent = getIntent();
        String key = intent.getStringExtra("key");
        if (!TextUtils.isEmpty(key)) {
            bitmap = BitmapCache.getInstance().getBitmap(key);
        }
        if (TextUtils.isEmpty(mPath)) {
            if (Intent.ACTION_VIEW == intent.getAction()) {
                String path = IntentFile.getPath(this, intent.getData());
                if (TextUtils.isEmpty(path) && intent.getData() != null) {
                    path = intent.getData().toString();
                }

                mPath = path;
                name = getIntent().getStringExtra("name");
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra(INTENT_PATH))) {
                    mPath = getIntent().getStringExtra(INTENT_PATH);
                }
                name = getIntent().getStringExtra(INTENT_NAME);
            }
        }
        if (TextUtils.isEmpty(name)) {
            name = "ocr";
        }
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
        Log.e("OcrActivity", e.getMessage(), e);
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
        StringBuilder sb = new StringBuilder();
        for (BaseResultModel baseResultModel : models) {
            sb.append(baseResultModel.getName()).append("\n");
        }
        File dir = FileUtils.getStorageDir("amupdf");
        if (dir != null && dir.exists()) {
            String filePath = dir.getAbsolutePath() + File.separator + name + ".txt";
            try {
                StreamUtils.copyStringToFile(sb.toString(), filePath);
                Toast.makeText(this, "保存成功:" + filePath, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "保存失败:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void copy(List<BaseResultModel> models) {
        StringBuilder sb = new StringBuilder();
        for (BaseResultModel baseResultModel : models) {
            sb.append(baseResultModel.getName()).append("\n");
        }
        if (null == clipboardManager) {
            clipboardManager = (ClipboardManager) App.Companion.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        }
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Label", sb));
        Toast.makeText(App.Companion.getInstance(), "save text to clipboard", Toast.LENGTH_SHORT).show();
    }
}
