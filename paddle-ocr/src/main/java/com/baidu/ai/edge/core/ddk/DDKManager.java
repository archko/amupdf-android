package com.baidu.ai.edge.core.ddk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.BaseManager;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.IStatisticsResultModel;
import com.baidu.ai.edge.core.base.JniParam;
import com.baidu.ai.edge.core.base.StatisticsResultModelImpl;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.util.ImageUtil;
import com.baidu.ai.edge.core.util.TimerRecorder;
import java.util.ArrayList;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ddk/DDKManager.class */
public class DDKManager extends BaseManager implements ClassifyInterface, DetectInterface {
    private int h;
    private DDKConfig i;

    public DDKManager(Context context, DDKConfig dDKConfig, String str) throws BaseException {
        super(context, new DDKJni(), dDKConfig, str);
        this.h = 0;
        this.i = dDKConfig;
        if (DDKJni.a() != null) {
            this.h = -1;
            return;
        }
        loadModelFromAssets(this.d);
        Log.i("DDKManager", "Build.HARDWARE: " + Build.HARDWARE);
        this.h = 2;
    }

    private List<ClassificationResultModel> a(float[] fArr) throws BaseException {
        return DDKJni.runMixModelSync(new DDKModelInfo(this.i.getImageHeight(), this.i.getImageWidth(), this.i.getCateNum()), fArr);
    }

    private List<DetectionResultModel> d(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        TimerRecorder.start();
        float[] pixels = DDKJni.getPixels(ImageUtil.resize(bitmap, this.i.getImageWidth(), this.i.getImageHeight()), this.i.getImgMeans(), this.i.getScales(), this.i.isHWC(), this.i.isRGB());
        DDKModelInfo dDKModelInfo = new DDKModelInfo(this.i.getImageHeight(), this.i.getImageWidth(), this.i.getCateNum());
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        TimerRecorder.start();
        long end = TimerRecorder.end();
        Log.i("DDKManager", "[stat] detect preprocessTime time:" + end);
        TimerRecorder.start();
        float[] runMixModelDetectSync = DDKJni.runMixModelDetectSync(dDKModelInfo, pixels);
        long end2 = TimerRecorder.end();
        Log.i("DDKManager", "[stat] forwardTime time:" + end2);
        TimerRecorder.start();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < runMixModelDetectSync.length; i += 6) {
            float f2 = runMixModelDetectSync[i + 4];
            if (f2 >= f) {
                float f3 = width;
                float f4 = height;
                Rect rect = new Rect((int) (runMixModelDetectSync[i] * f3), (int) (runMixModelDetectSync[i + 1] * f4), (int) (runMixModelDetectSync[i + 2] * f3), (int) (runMixModelDetectSync[i + 3] * f4));
                int i2 = (int) runMixModelDetectSync[i + 5];
                DetectionResultModel detectionResultModel = new DetectionResultModel();
                detectionResultModel.setBounds(rect);
                detectionResultModel.setConfidence(f2);
                detectionResultModel.setLabelIndex(i2);
                detectionResultModel.setLabel(this.i.getLabels()[i2]);
                arrayList.add(detectionResultModel);
            }
        }
        long end3 = TimerRecorder.end();
        if (eVar != null) {
            eVar.setPreprocessTime(end);
            eVar.setForwardTime(end2);
            eVar.setPostprocessTime(end3);
            eVar.setResultModel(arrayList);
        }
        return arrayList;
    }

    private List<ClassificationResultModel> c(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        if (this.h == 2) {
            TimerRecorder.start();
            float[] pixels = DDKJni.getPixels(ImageUtil.resize(bitmap, this.i.getImageWidth(), this.i.getImageHeight()), this.i.getImgMeans(), this.i.getScales(), this.i.isHWC(), this.i.isRGB());
            long end = TimerRecorder.end();
            Log.i("DDKManager", "[stat] preprocess time:" + end);
            TimerRecorder.start();
            List<ClassificationResultModel> a2 = a(pixels);
            long end2 = TimerRecorder.end();
            Log.i("DDKManager", "[stat] forward time:" + end2);
            TimerRecorder.start();
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < a2.size(); i++) {
                ClassificationResultModel classificationResultModel = a2.get(i);
                if (classificationResultModel.getConfidence() >= f) {
                    classificationResultModel.setLabel(this.i.getLabels()[classificationResultModel.getLabelIndex()]);
                    arrayList.add(classificationResultModel);
                }
            }
            long end3 = TimerRecorder.end();
            if (eVar != null) {
                eVar.setPreprocessTime(end);
                eVar.setForwardTime(end2);
                eVar.setPostprocessTime(end3);
                eVar.setResultModel(arrayList);
            }
            c();
            return arrayList;
        }
        throw DDKJni.a();
    }

    public void loadModelFromAssets(String str) throws BaseException {
        JniParam b = b();
        if (this.i.getModelFileAssetPath() == null) {
            throw new BaseException(Consts.EC_LOAD_DDK_MODEL_FAIL, "该模型在您的设备上不可用");
        }
        b.put("modelFileAssetPath", this.i.getModelFileAssetPath());
        Log.i("DDKManager", "load 200 model file: " + this.i.getModelFileAssetPath());
        Context context = this.b;
        if (DDKJni.loadMixModelSync(context, context.getAssets(), b) != 0) {
            throw new BaseException(Consts.EC_LOAD_DDK_MODEL_FAIL, "加载DDK模型文件失败");
        }
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap, float f) throws BaseException {
        return d(bitmap, f, null);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap) throws BaseException {
        return detect(bitmap, this.i.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected float[] a(Bitmap bitmap, JniParam jniParam, int i) {
        return new float[0];
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public IStatisticsResultModel detectPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        d(bitmap, this.i.getRecommendedConfidence(), eVar);
        return eVar;
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected void e() throws CallException {
        DDKJni.b();
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public void destroy() throws BaseException {
        int unloadMixModelSync = DDKJni.unloadMixModelSync();
        DDKJni.deactivateInstance(this.b);
        if (unloadMixModelSync != 0) {
            throw new BaseException(Consts.EC_UNLOAD_DDK_MODEL_FAIL, "卸载DDK模型失败");
        }
        super.destroy();
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap) throws BaseException {
        return classify(bitmap, this.i.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap, float f) throws BaseException {
        return c(bitmap, f, null);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public IStatisticsResultModel classifyPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        c(bitmap, this.i.getRecommendedConfidence(), eVar);
        return eVar;
    }
}
