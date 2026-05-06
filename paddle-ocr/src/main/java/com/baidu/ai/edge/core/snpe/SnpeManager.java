package com.baidu.ai.edge.core.snpe;

import android.content.Context;
import android.graphics.Bitmap;
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
import java.util.Arrays;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/snpe/SnpeManager.class */
public class SnpeManager extends BaseManager implements ClassifyInterface, DetectInterface {
    private static volatile boolean j = false;
    private static ArrayList<Integer> k;
    private SnpeConfig h;
    private long i;

    /* JADX WARN: Type inference failed for: r0v2, types: [java.lang.Throwable, java.lang.Class<com.baidu.ai.edge.core.snpe.SnpeManager>] */
    public SnpeManager(Context context, SnpeConfig snpeConfig, String str) throws BaseException {
        super(context, new SnpeJni(), snpeConfig, str);
        int i;
        if (snpeConfig.isAutocheckQcom()) {
            if (!"qcom".equalsIgnoreCase(Build.HARDWARE)) {
                throw new SnpeQcomNotSupportException(Consts.EC_SNPE_NOT_QCOM, "Build.HARDWARE is not qcom;" + Build.HARDWARE);
            }
            Log.i("SnpeManager", "cpu support : " + Build.HARDWARE);
        }
        synchronized (SnpeManager.class) {
            if (j) {
                throw new CallException(Consts.EC_BASE_MANAGER_MULTI_INSTANCES, "only one active instance of SnpeManager is allowed, please destory() the old one");
            }
            getAvailableRuntimes(context);
            j = true;
        }
        int[] snpeRuntimesOrder = snpeConfig.getSnpeRuntimesOrder();
        int length = snpeRuntimesOrder.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                i = -1;
                break;
            }
            i = snpeRuntimesOrder[i2];
            if (k.contains(Integer.valueOf(i))) {
                break;
            }
            i2++;
        }
        if (i < 0) {
            throw new SnpeQcomNotSupportException(Consts.EC_SNPE_RUNTIME_ALLOWED_NONE, "your allowed runtimes is:" + Arrays.toString(snpeConfig.getSnpeRuntimesOrder()));
        }
        this.h = snpeConfig;
        JniParam b = b();
        b.put("runtime", i);
        this.i = SnpeJni.a(context, context.getAssets(), b);
    }

    public static List<Integer> getAvailableRuntimes(Context context) {
        if (k == null) {
            SnpeJni.setDspRuntimePath(context.getApplicationInfo().nativeLibraryDir);
            k = SnpeJni.a();
        }
        return new ArrayList(k);
    }

    private List<ClassificationResultModel> a(float[] fArr, float f, String[] strArr) {
        ArrayList arrayList = new ArrayList(fArr.length / 2);
        for (int i = 0; i < fArr.length / 2; i++) {
            int i2 = i * 2;
            int round = Math.round(fArr[i2]);
            float f2 = fArr[i2 + 1];
            if (f2 >= f) {
                ClassificationResultModel classificationResultModel = new ClassificationResultModel(round < strArr.length ? strArr[round] : "UNKNOWN:" + round, f2);
                classificationResultModel.setLabelIndex(round);
                arrayList.add(classificationResultModel);
            }
        }
        return arrayList;
    }

    private List<ClassificationResultModel> c(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        a();
        SnpeConfig snpeConfig = this.h;
        TimerRecorder.start();
        float[] pixels = SnpeJni.getPixels(ImageUtil.resize(bitmap, snpeConfig.getImageWidth(), snpeConfig.getImageHeight()), snpeConfig.getImgMeans(), snpeConfig.getScales(), snpeConfig.isHWC(), snpeConfig.isRGB());
        long end = TimerRecorder.end();
        Log.i("SnpeManager", "[stat] preprocess time:" + end);
        TimerRecorder.start();
        float[] a2 = SnpeJni.a(this.i, pixels, 0);
        long end2 = TimerRecorder.end();
        TimerRecorder.start();
        List<ClassificationResultModel> a3 = a(a2, f, snpeConfig.getLabels());
        long end3 = TimerRecorder.end();
        Log.i("SnpeManager", "[stat] forward time:" + end2);
        if (eVar != null) {
            eVar.setPreprocessTime(end);
            eVar.setForwardTime(end2);
            eVar.setPostprocessTime(end3);
            eVar.setResultModel(a3);
        }
        c();
        return a3;
    }

    private List<DetectionResultModel> d(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        a();
        SnpeConfig snpeConfig = this.h;
        TimerRecorder.start();
        float[] pixels = SnpeJni.getPixels(ImageUtil.resize(bitmap, snpeConfig.getImageWidth(), snpeConfig.getImageHeight()), snpeConfig.getImgMeans(), snpeConfig.getScales(), snpeConfig.isHWC(), snpeConfig.isRGB());
        long end = TimerRecorder.end();
        Log.i("SnpeManager", "[stat] preprocess time:" + end);
        TimerRecorder.start();
        float[] execute = SnpeJni.execute(this.i, pixels, 1, f);
        long end2 = TimerRecorder.end();
        TimerRecorder.start();
        Log.i("SnpeManager", "result " + execute.length);
        List<DetectionResultModel> a2 = a(execute, this.h.getLabels(), bitmap.getWidth(), bitmap.getHeight());
        long end3 = TimerRecorder.end();
        Log.i("SnpeManager", "[stat] forward time:" + end2);
        if (eVar != null) {
            eVar.setPreprocessTime(end);
            eVar.setForwardTime(end2);
            eVar.setPostprocessTime(end3);
            eVar.setResultModel(a2);
        }
        c();
        return a2;
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap) throws BaseException {
        return classify(bitmap, this.h.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public List<ClassificationResultModel> classify(Bitmap bitmap, float f) throws BaseException {
        return c(bitmap, f, null);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public IStatisticsResultModel classifyPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        c(bitmap, this.h.getRecommendedConfidence(), eVar);
        return eVar;
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap, float f) throws BaseException {
        return d(bitmap, f, null);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public List<DetectionResultModel> detect(Bitmap bitmap) throws BaseException {
        return detect(bitmap, this.h.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected float[] a(Bitmap bitmap, JniParam jniParam, int i) {
        return new float[0];
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public IStatisticsResultModel detectPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        d(bitmap, this.h.getRecommendedConfidence(), eVar);
        return eVar;
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected void e() throws CallException {
        SnpeJni.b();
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public void destroy() throws BaseException {
        a();
        long j2 = this.i;
        if (j2 != 0) {
            SnpeJni.destory(j2);
            SnpeJni.deactivateInstance(this.b);
        }
        j = false;
        super.destroy();
    }
}
