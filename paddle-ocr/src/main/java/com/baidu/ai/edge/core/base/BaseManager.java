package com.baidu.ai.edge.core.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import com.baidu.ai.edge.core.classify.ClassifyPostProcess;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.ddk.DavinciManager;
import com.baidu.ai.edge.core.detect.DetectPostProcess;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import a.a.a.a.a.a.StatManager;
import com.baidu.ai.edge.core.util.ImageUtil;
import com.baidu.ai.edge.core.util.TimeRecorderNew;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/BaseManager.class */
public abstract class BaseManager {
    public static final String VERSION = "0.10.7";

    /* renamed from: a  reason: collision with root package name */
    protected ActivateManager f10a;
    protected Context b;
    protected BaseConfig c;
    protected String d;
    private StatManager e;
    protected String f;
    private boolean g = false;

    public BaseManager(Context context, ISDKJni iSDKJni, BaseConfig baseConfig, String str) throws CallException {
        a(context);
        this.c = baseConfig;
        this.d = str;
        this.f = getClass().getSimpleName();
        boolean d = d();
        if (d) {
            this.d = null;
        } else if (str == null) {
            Log.e("BaseManager", "serial number is NULL");
            throw new CallException(Consts.EC_OFFLINE_SERIAL_NULL_ERROR, "serial number is NULL");
        }
        this.b = context;
        Log.i("BaseManager", "new Manager with " + baseConfig.getMid() + " " + baseConfig.getRid());
        this.f10a = new ActivateManager(context, baseConfig);
        e();
        try {
            String a2 = a(str, d);
            if (baseConfig.getRid() == -1 || baseConfig.getMid() == -1 || baseConfig.getModelEncValue() == 1200) {
                return;
            }
            this.e = new StatManager(context, iSDKJni, baseConfig, a2);
            this.e.c();
        } catch (Exception e) {
            throw new CallException(Consts.EC_OFFLINE_AUTH_ERROR, e.getMessage(), e.getCause());
        }
    }

    private String a(String str, boolean z) throws IOException, BaseException {
        if (this instanceof InferManager) {
            return this.f10a.activate(str, 100, z);
        }
        if (this instanceof SnpeManager) {
            return this.f10a.activate(str, ActivateManager.JNILIB_TYPE_SNPE, z);
        }
        if (this instanceof DDKManager) {
            return this.f10a.activate(str, 102, z);
        }
        if (this instanceof DavinciManager) {
            return this.f10a.activate(str, ActivateManager.JNILIB_TYPE_DAVINCI, z);
        }
        return null;
    }

    private void a(Context context) throws CallException {
        for (String str : new String[]{"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.READ_PHONE_STATE"}) {
            if (ContextCompat.checkSelfPermission(context, str) != 0) {
                throw new CallException(Consts.EC_BASE_NO_PERMISSION, "Please allow permission:" + str);
            }
        }
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            throw new CallException(Consts.EC_BASE_NO_PERMISSION, "Please allow all files access:");
        }
    }

    protected abstract void e() throws CallException;

    protected boolean d() {
        String authType = this.c.getAuthType();
        return (authType == null || authType.isEmpty()) ? Consts.PROD_EASYEDGE_FREE.equals(this.c.getProduct()) : "no-auth".equals(authType);
    }

    public IStatisticsResultModel detectPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        b(bitmap, this.c.getRecommendedConfidence(), eVar);
        return eVar;
    }

    public List<DetectionResultModel> detect(Bitmap bitmap, float f) throws BaseException {
        return b(bitmap, f, null);
    }

    public List<DetectionResultModel> detect(Bitmap bitmap) throws BaseException {
        return detect(bitmap, this.c.getRecommendedConfidence());
    }

    public IStatisticsResultModel classifyPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        a(bitmap, this.c.getRecommendedConfidence(), eVar);
        return eVar;
    }

    public List<ClassificationResultModel> classify(Bitmap bitmap) throws BaseException {
        return classify(bitmap, this.c.getRecommendedConfidence());
    }

    public List<ClassificationResultModel> classify(Bitmap bitmap, float f) throws BaseException {
        return a(bitmap, f, null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<DetectionResultModel> b(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        int[] iArr = new int[2];
        ImageArray a2 = a(bitmap, f, eVar, 2, iArr);
        float[] b = a2.b();
        DetectPostProcess aVar = new DetectPostProcess(getClass(), this.c, bitmap.getWidth(), bitmap.getHeight(), iArr[0], iArr[1], a2.a());
        TimeRecorderNew timeRecorderNew = new TimeRecorderNew();
        List<DetectionResultModel> a3 = aVar.a(b, f);
        long end = timeRecorderNew.end();
        if (eVar != null) {
            eVar.setPostprocessTime(end);
            eVar.setResultModel(a3);
        }
        return a3;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<ClassificationResultModel> a(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        int[] iArr = new int[2];
        float[] b = a(bitmap, f, eVar, 2, iArr).b();
        ClassifyPostProcess aVar = new ClassifyPostProcess(getClass(), this.c, bitmap.getWidth(), bitmap.getHeight(), iArr[0], iArr[1]);
        TimeRecorderNew timeRecorderNew = new TimeRecorderNew();
        List<ClassificationResultModel> a2 = aVar.a(b, f);
        long end = timeRecorderNew.end();
        if (eVar != null) {
            eVar.setPostprocessTime(end);
            eVar.setResultModel(a2);
        }
        return a2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ImageArray a(Bitmap bitmap, float f, IStatisticsResultModel iStatisticsResultModel, int i, int[] iArr) throws BaseException {
        a();
        Log.i(this.f, "predict " + i + ": confidence: " + f);
        TimeRecorderNew timeRecorderNew = new TimeRecorderNew();
        JniParam a2 = a(bitmap, i, f, iArr);
        ImageArray cVar = new ImageArray(a(bitmap, a2, i));
        long j = a2.getLong("preprocessEndTime");
        if (a2.containsKey("extraNetFlag")) {
            cVar.a((int) a2.getLong("extraNetFlag"));
        }
        long checkpoint = timeRecorderNew.checkpoint(j);
        Log.i(this.f, "[stat]preprocess time: " + checkpoint);
        long end = timeRecorderNew.end();
        Log.i(this.f, "[stat]forward time: " + end);
        if (iStatisticsResultModel != null) {
            iStatisticsResultModel.setPreprocessTime(checkpoint);
            iStatisticsResultModel.setForwardTime(end);
        }
        c();
        return cVar;
    }

    protected abstract float[] a(Bitmap bitmap, JniParam jniParam, int i) throws BaseException;

    protected JniParam a(Bitmap bitmap, int[] iArr, int i) {
        PreprocessConfig preprocessConfig = this.c.getPreprocessConfig();
        Pair<Integer, Integer> pair = null;
        if (i == 100) {
            pair = ImageUtil.calcWithStep(bitmap, this.c.getMaxSize(), 32);
        } else if (this.c.getNType() == 102 || this.c.getNType() == 900102 || this.c.getNType() == 2010 || "keep_ratio".equalsIgnoreCase(this.c.getPreprocessConfig().r())) {
            pair = ImageUtil.calcTarget(bitmap, preprocessConfig.t(), preprocessConfig.g());
        } else if (this.c.getNType() == 11002) {
            pair = ImageUtil.calcShrinkSize(bitmap.getWidth(), bitmap.getHeight());
        } else if ("keep_ratio2".equalsIgnoreCase(preprocessConfig.r())) {
            pair = ImageUtil.calcTargetSizeForKeepRatio2(bitmap, preprocessConfig.q(), preprocessConfig.p());
        }
        Pair<Integer, Integer> pair2 = pair;
        int q = preprocessConfig.q();
        int p = preprocessConfig.p();
        if (pair2 != null) {
            Pair<Integer, Integer> pair3 = pair;
            q = pair3.first;
            p = pair3.second;
        }
        if (iArr != null) {
            iArr[0] = preprocessConfig.u() ? preprocessConfig.q() : q;
            iArr[1] = preprocessConfig.u() ? preprocessConfig.p() : p;
        }
        JniParam a2 = JniParamBuilder.a(preprocessConfig, q, p);
        if (i == 100) {
            a2.put("ocrRecWidth", preprocessConfig.j());
            a2.put("ocrRecHeight", preprocessConfig.i());
            a2.put("ocrRecBatchNum", preprocessConfig.h());
        }
        return a2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public JniParam a(Bitmap bitmap, int i, float f, int[] iArr) {
        JniParam jniParam = new JniParam();
        jniParam.put("product", this.c.j);
        jniParam.put("originWidth", bitmap.getWidth());
        jniParam.put("originHeight", bitmap.getHeight());
        jniParam.put("preprocessObj", a(bitmap, iArr, i));
        jniParam.put("modelType", i);
        jniParam.put("nType", this.c.getNType());
        jniParam.put("confidence", f);
        jniParam.put("extraDetection", this.c.getExtraDetectionJson());
        jniParam.put("classNum", this.c.getLabels().length);
        return jniParam;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<DetectionResultModel> a(float[] fArr, String[] strArr, int i, int i2) {
        String str;
        int length = fArr.length / 7;
        ArrayList arrayList = new ArrayList(length);
        for (int i3 = 0; i3 < length; i3++) {
            int i4 = i3 * 7;
            int round = Math.round(fArr[i4 + 1]);
            if (round < 0 || round >= strArr.length) {
                Log.e("SnpeManager", "label index out of bound , index : " + round + " ,at :" + i3);
                str = "UNKNOWN";
            } else {
                str = strArr[round];
            }
            float f = i;
            float f2 = i2;
            DetectionResultModel detectionResultModel = new DetectionResultModel(str, fArr[i4 + 2], new Rect(Math.round(fArr[i4 + 3] * f), Math.round(fArr[i4 + 4] * f2), Math.round(fArr[i4 + 5] * f), Math.round(fArr[i4 + 6] * f2)));
            detectionResultModel.setLabelIndex(round);
            arrayList.add(detectionResultModel);
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void c() {
        StatManager aVar = this.e;
        if (aVar != null) {
            aVar.a();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void destroy() throws BaseException {
        this.g = true;
        this.f10a.terminate();
        Log.i("InferManager", "pointer destroy");
        StatManager aVar = this.e;
        if (aVar != null) {
            aVar.b();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public JniParam b() {
        JniParam fillCommonAuthParam = this.f10a.fillCommonAuthParam(this.d);
        fillCommonAuthParam.put("modelEncVal", this.c.getModelEncValue());
        fillCommonAuthParam.put("modelType", this.c.getModelType());
        fillCommonAuthParam.put("modelFileAssetPath", this.c.getModelFileAssetPath());
        fillCommonAuthParam.put("nType", this.c.getNType());
        fillCommonAuthParam.put("skipDecrypt", d());
        return fillCommonAuthParam;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void a() throws CallException {
        if (this.g) {
            throw new CallException(Consts.EC_BASE_MANAGER_HAS_DETORYED, "this instance is destoryed");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String a(int i) {
        String[] labels = this.c.getLabels();
        return (i < 0 || i >= labels.length) ? "UNKNOWN" : labels[i];
    }
}
