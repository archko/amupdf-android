package com.baidu.ai.edge.core.infer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.Pair;
import com.baidu.ai.edge.core.base.BaseException;
import com.baidu.ai.edge.core.base.BaseManager;
import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.base.IStatisticsResultModel;
import com.baidu.ai.edge.core.base.JniParam;
import com.baidu.ai.edge.core.base.StatisticsResultModelImpl;
import com.baidu.ai.edge.core.classify.ClassificationResultModel;
import com.baidu.ai.edge.core.classify.ClassifyInterface;
import com.baidu.ai.edge.core.detect.DetectException;
import com.baidu.ai.edge.core.detect.DetectInterface;
import com.baidu.ai.edge.core.detect.DetectionResultModel;
import com.baidu.ai.edge.core.ocr.OcrInterface;
import com.baidu.ai.edge.core.ocr.OcrResultModel;
import com.baidu.ai.edge.core.pose.PoseInterface;
import com.baidu.ai.edge.core.pose.PoseResultModel;
import com.baidu.ai.edge.core.segment.SegmentInterface;
import com.baidu.ai.edge.core.segment.SegmentationResultModel;
import com.baidu.ai.edge.core.util.ImageUtil;
import com.baidu.ai.edge.core.util.TimeRecorderNew;
import com.baidu.ai.edge.core.util.TimerRecorder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/infer/InferManager.class */
public final class InferManager extends BaseManager implements ClassifyInterface, DetectInterface, SegmentInterface, OcrInterface, PoseInterface {
    private static volatile boolean k = false;
    private InferConfig h;
    private long i;
    private int j;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/infer/InferManager$a.class */
    public class a implements Comparator<ClassificationResultModel> {
        a(InferManager inferManager) {
        }

        @Override // java.util.Comparator
        /* renamed from: a */
        public int compare(ClassificationResultModel classificationResultModel, ClassificationResultModel classificationResultModel2) {
            float confidence = classificationResultModel2.getConfidence() - classificationResultModel.getConfidence();
            if (confidence > 0.0f) {
                return 1;
            }
            return confidence < 0.0f ? -1 : 0;
        }
    }

    /* JADX WARN: Type inference failed for: r0v1, types: [java.lang.Throwable, java.lang.Class<com.baidu.ai.edge.core.infer.InferManager>] */
    public InferManager(Context context, InferConfig inferConfig, String str) throws BaseException {
        super(context, new InferLiteJni(), inferConfig, str);
        synchronized (InferManager.class) {
            if (k) {
                throw new CallException(Consts.EC_BASE_MANAGER_MULTI_INSTANCES, "only one active instance of InferManager is allowed, please destory() the old one");
            }
            k = true;
        }
        if (inferConfig.getNType() == 102 || inferConfig.getNType() == 900102 || inferConfig.getNType() == 2010) {
            inferConfig.getPreprocessConfig().a(32);
        } else {
            inferConfig.getPreprocessConfig().a(0);
        }
        this.h = inferConfig;
        if (inferConfig.getModelFileAssetPath() == null) {
            throw new CallException(Consts.EC_BASE_DETECT_MANAGER_ASSET_MODEL_NULL, "model asset file path is NULL");
        }
        if (!inferConfig.isOptModel() && inferConfig.getParamFileAssetPath() == null) {
            throw new CallException(Consts.EC_BASE_DETECT_MANAGER_ASSET_MODEL_NULL, "param asset file path is NULL");
        }
        Log.i("InferManager", "infer thread: " + inferConfig.getThread());
        Log.i("InferManager", "infer getParamFileAssetPath: " + inferConfig.getParamFileAssetPath());
        Log.i("InferManager", "infer getModelFileAssetPath: " + inferConfig.getModelFileAssetPath());
        a(context, this.d);
    }

    public static boolean isSupportOpencl() throws CallException {
        return InferLiteJni.a();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v29 */
    /* JADX WARN: Type inference failed for: r0v30 */
    /* JADX WARN: Type inference failed for: r0v9, types: [java.lang.Exception] */
    private void a(Context context, String str) throws BaseException {
        InferManager inferManager;
        long loadCombinedMemoryUC;
        JniParam b = b();
        b.put("thread", this.h.getThread());
        if (this.j == 3) {
            b.put("opencl_tune", Boolean.valueOf(((ArmGpuConfig) this.c).isOpenclTune()));
        }
        Log.i("InferManager", "serial num length is " + (str == null ? null : Integer.valueOf(str.length())));
        int modelType = this.h.getModelType();
        JniParam r0 = b;
        if (modelType == 100) {
            if (this.h.getExtraModelFilePath() == null) {
                throw new DetectException(Consts.EC_BASE_JNI_MODELFILE_READERROR, "extraModelFileAssetPath must be set for this model type :" + modelType);
            }
            r0.put("extraModelFileAssetPath", this.h.getExtraModelFilePath());
        }
        try {
            if (this.h.isOptModel()) {
                inferManager = this;
                loadCombinedMemoryUC = InferLiteJni.loadCombinedMemoryNB(context, context.getAssets(), b);
            } else {
                inferManager = this;
                b.put("paramFileAssetPath", this.h.getParamFileAssetPath());
                loadCombinedMemoryUC = InferLiteJni.loadCombinedMemoryUC(context, context.getAssets(), b);
            }
            inferManager.i = loadCombinedMemoryUC;
            Log.i("InferManager", "loadCombinedMemory success isOptModel: " + this.h.isOptModel());
        } catch (Exception e) {
            e.printStackTrace();
            throw BaseException.transform(e, "init models failed:");
        }
    }

    private ArrayList<OcrResultModel> a(float[] fArr, float f) {
        ArrayList<OcrResultModel> arrayList = new ArrayList<>();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= fArr.length) {
                return arrayList;
            }
            int round = Math.round(fArr[i2]);
            int round2 = Math.round(fArr[i2 + 1]);
            int i3 = i2 + 2;
            if (fArr[i3] >= f) {
                arrayList.add(a(fArr, i3, round, round2));
            }
            i = i2 + (round * 2) + 3 + round2;
        }
    }

    private OcrResultModel a(float[] fArr, int i, int i2, int i3) {
        OcrResultModel ocrResultModel = new OcrResultModel();
        ocrResultModel.setConfidence(fArr[i]);
        int i4 = i + 1;
        for (int i5 = 0; i5 < i2; i5++) {
            int i6 = i4 + (i5 * 2);
            ocrResultModel.addPoints(Math.round(fArr[i6]), Math.round(fArr[i6 + 1]));
        }
        int i7 = i4 + (i2 * 2);
        StringBuffer stringBuffer = new StringBuffer(i3);
        String[] labels = this.h.getLabels();
        for (int i8 = 0; i8 < i3; i8++) {
            int round = Math.round(fArr[i7 + i8]);
            ocrResultModel.addWordIndex(round);
            stringBuffer.append(round < labels.length ? labels[round] : "?");
            if (round >= labels.length) {
                Log.e("InferManager", "UNKNOWN index :" + round + "; total:" + labels.length);
            }
        }
        ocrResultModel.setLabel(stringBuffer.toString());
        return ocrResultModel;
    }

    private List<PoseResultModel> b(Bitmap bitmap, float f, IStatisticsResultModel iStatisticsResultModel) throws BaseException {
        List<PoseResultModel> a2 = a(a(bitmap, f, iStatisticsResultModel, PoseInterface.POSE_TYPE, new int[2]).b(), bitmap);
        long end = new TimeRecorderNew().end();
        if (iStatisticsResultModel != null) {
            iStatisticsResultModel.setPostprocessTime(end);
            iStatisticsResultModel.setResultModel(a2);
        }
        return a2;
    }

    private List<PoseResultModel> a(float[] fArr, Bitmap bitmap) {
        ArrayList arrayList = new ArrayList();
        boolean z = this.h.getNType() == 20041;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= fArr.length) {
                return arrayList;
            }
            PoseResultModel poseResultModel = new PoseResultModel();
            poseResultModel.setLabelIndex(0);
            poseResultModel.setLabel(a(0));
            poseResultModel.setHasGroups(z);
            int i3 = i2 + 1;
            poseResultModel.setIndex(Math.round(fArr[i2]));
            int i4 = i3 + 1;
            poseResultModel.setGroupIndex(Math.round(fArr[i3]));
            int i5 = i4 + 1;
            poseResultModel.setConfidence(fArr[i4]);
            int i6 = i5 + 1;
            int i7 = i6 + 1;
            poseResultModel.setPoint(new Point(Math.round(fArr[i5] * bitmap.getWidth()), Math.round(fArr[i6] * bitmap.getHeight())));
            ArrayList arrayList2 = new ArrayList();
            int i8 = i7 + 1;
            int round = Math.round(fArr[i7]);
            int i9 = 0;
            while (i9 < round) {
                PoseResultModel poseResultModel2 = new PoseResultModel();
                int i10 = i8 + 1;
                poseResultModel2.setIndex(Math.round(fArr[i8]));
                poseResultModel2.setGroupIndex(poseResultModel.getGroupIndex());
                int i11 = i10 + 1;
                poseResultModel2.setConfidence(fArr[i10]);
                int i12 = i11 + 1;
                poseResultModel2.setPoint(new Point(Math.round(fArr[i11] * bitmap.getWidth()), Math.round(fArr[i12] * bitmap.getHeight())));
                arrayList2.add(poseResultModel2);
                i9++;
                i8 = i12 + 1;
            }
            poseResultModel.setPairs(arrayList2);
            arrayList.add(poseResultModel);
            i = i8;
        }
    }

    private List<ClassificationResultModel> c(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        a();
        float[] fArr = {1.0f, 3.0f, this.h.getImageHeight(), this.h.getImageWidth()};
        TimerRecorder.start();
        float[] pixels = InferLiteJni.getPixels(ImageUtil.resize(bitmap, this.h.getImageWidth(), this.h.getImageHeight()), this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), 0);
        long end = TimerRecorder.end();
        Log.i("InferManager classify", "[stat] preprocess time:" + end);
        TimerRecorder.start();
        float[] predictImage = InferLiteJni.predictImage(this.i, pixels, fArr, this.h.getNType());
        long end2 = TimerRecorder.end();
        Log.i("InferManager classify", "[stat] forward time:" + end2);
        TimerRecorder.start();
        List<ClassificationResultModel> a2 = a(predictImage, f, this.h.getLabels());
        long end3 = TimerRecorder.end();
        Log.i("InferManager classify", "[stat] postprocessTime time:" + end3);
        if (eVar != null) {
            eVar.setPreprocessTime(end);
            eVar.setForwardTime(end2);
            eVar.setPostprocessTime(end3);
            eVar.setResultModel(a2);
        }
        c();
        return a2;
    }

    private List<DetectionResultModel> a(Bitmap bitmap, float f, IStatisticsResultModel iStatisticsResultModel) throws BaseException {
        Pair<Bitmap, Float> pair;
        float[] pixels;
        int round;
        int round2;
        int round3;
        int round4;
        a();
        Log.i("InferManager detect", "detect confidence: " + f);
        TimerRecorder.start();
        float[] fArr = new float[4];
        fArr[0] = 1.0f;
        fArr[1] = 3.0f;
        pair = null;
        Log.i("InferManager detect", "[preprocess] detect target: " + this.h.getTargetSize());
        if (this.h.getNType() == 102 || this.h.getNType() == 900102) {
            pair = ImageUtil.resizeTarget(bitmap, this.h.getTargetSize(), this.h.getMaxSize());
            fArr[2] = ((Bitmap) pair.first).getHeight();
            fArr[3] = ((Bitmap) pair.first).getWidth();
            pixels = InferLiteJni.getPixels((Bitmap) pair.first, this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), 32);
            Log.i("pixel size", "" + pixels.length);
        } else if (this.h.getNType() == 11002) {
            Pair<Integer, Integer> calcShrinkSize = ImageUtil.calcShrinkSize(bitmap.getWidth(), bitmap.getHeight());
            int intValue = ((Integer) calcShrinkSize.first).intValue();
            int intValue2 = ((Integer) calcShrinkSize.second).intValue();
            pixels = InferLiteJni.getPixels(ImageUtil.resize(bitmap, intValue, intValue2), this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), 0);
            fArr[2] = intValue2;
            fArr[3] = intValue;
        } else {
            pixels = InferLiteJni.getPixels(ImageUtil.resize(bitmap, this.h.getImageWidth(), this.h.getImageHeight()), this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), 0);
            fArr[2] = this.h.getImageHeight();
            fArr[3] = this.h.getImageWidth();
        }
        long end = TimerRecorder.end();
        Log.i("InferManager detect", "[stat] preprocess time:" + end);
        TimerRecorder.start();
        float[] predictImage = InferLiteJni.predictImage(this.i, pixels, fArr, this.h.getNType());
        long end2 = TimerRecorder.end();
        Log.i("InferManager detect", "[stat] forward time:" + end2);
        TimerRecorder.start();
        int length = predictImage.length / 6;
        ArrayList arrayList = new ArrayList(length);
        String[] labels = this.h.getLabels();
        for (int i = 0; i < length; i++) {
            int i2 = i * 6;
            int round5 = Math.round(predictImage[i2 + 0]);
            if (this.h.getNType() == 101) {
                round = Math.round((predictImage[i2 + 2] / this.h.getImageWidth()) * bitmap.getWidth());
                round2 = Math.round((predictImage[i2 + 3] / this.h.getImageHeight()) * bitmap.getHeight());
                round3 = Math.round((predictImage[i2 + 4] / this.h.getImageWidth()) * bitmap.getWidth());
                round4 = Math.round((predictImage[i2 + 5] / this.h.getImageHeight()) * bitmap.getHeight());
            } else if (this.h.getNType() != 102 || pair == null) {
                round = Math.round(predictImage[i2 + 2] * bitmap.getWidth());
                round2 = Math.round(predictImage[i2 + 3] * bitmap.getHeight());
                round3 = Math.round(predictImage[i2 + 4] * bitmap.getWidth());
                round4 = Math.round(predictImage[i2 + 5] * bitmap.getHeight());
            } else {
                round = Math.round((predictImage[i2 + 2] / ((Bitmap) pair.first).getWidth()) * bitmap.getWidth());
                round2 = Math.round((predictImage[i2 + 3] / ((Bitmap) pair.first).getHeight()) * bitmap.getHeight());
                round3 = Math.round((predictImage[i2 + 4] / ((Bitmap) pair.first).getWidth()) * bitmap.getWidth());
                round4 = Math.round((predictImage[i2 + 5] / ((Bitmap) pair.first).getHeight()) * bitmap.getHeight());
            }
            if (round5 < 0 || round5 >= labels.length) {
                Log.e("InferManager", "label index out of bound , index : " + round5 + " ,at :" + i);
            } else {
                float f2 = predictImage[i2 + 1];
                if (f2 >= f) {
                    DetectionResultModel detectionResultModel = new DetectionResultModel(labels[round5], f2, new Rect(round, round2, round3, round4));
                    detectionResultModel.setLabelIndex(round5);
                    arrayList.add(detectionResultModel);
                }
            }
        }
        long end3 = TimerRecorder.end();
        Log.i("InferManager detect", "[stat] postprocess time:" + end3);
        if (iStatisticsResultModel != null) {
            iStatisticsResultModel.setPreprocessTime(end);
            iStatisticsResultModel.setForwardTime(end2);
            iStatisticsResultModel.setPostprocessTime(end3);
            iStatisticsResultModel.setResultModel(arrayList);
        }
        c();
        return arrayList;
    }

    private List<ClassificationResultModel> a(float[] fArr, float f, String[] strArr) {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i < fArr.length) {
            ClassificationResultModel classificationResultModel = new ClassificationResultModel(i < strArr.length ? strArr[i] : "UnKnown:" + i, fArr[i]);
            if (classificationResultModel.getConfidence() >= f) {
                classificationResultModel.setLabelIndex(i);
                arrayList.add(classificationResultModel);
            }
            i++;
        }
        Collections.sort(arrayList, new a(this));
        return arrayList;
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public synchronized void destroy() throws BaseException {
        a();
        k = false;
        InferLiteJni.clear(this.i);
        InferLiteJni.deactivateInstance(this.b);
        this.i = 0L;
        super.destroy();
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected void e() throws CallException {
        if (this.c instanceof ArmGpuConfig) {
            this.j = 3;
        } else {
            this.j = 0;
        }
        InferLiteJni.a(this.j);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager
    protected float[] a(Bitmap bitmap, JniParam jniParam, int i) throws BaseException {
        return InferLiteJni.predictNew(this.i, bitmap, jniParam);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.detect.DetectInterface
    public IStatisticsResultModel detectPro(Bitmap bitmap) throws BaseException {
        return super.detectPro(bitmap);
    }

    @Override // com.baidu.ai.edge.core.base.BaseManager, com.baidu.ai.edge.core.classify.ClassifyInterface
    public IStatisticsResultModel classifyPro(Bitmap bitmap) throws BaseException {
        return super.classifyPro(bitmap);
    }

    @Override // com.baidu.ai.edge.core.segment.SegmentInterface
    public List<SegmentationResultModel> segment(Bitmap bitmap) throws BaseException {
        return segment(bitmap, this.h.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.segment.SegmentInterface
    public List<SegmentationResultModel> segment(Bitmap bitmap, float f) throws BaseException {
        return segmentInternal(bitmap, f, null);
    }

    public List<SegmentationResultModel> segmentInternal(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        a();
        TimeRecorderNew timeRecorderNew = new TimeRecorderNew();
        JniParam a2 = a(bitmap, this.c.getModelType(), f, (int[]) null);
        ArrayList<SegmentationResultModel> predictImageSegmentNew = InferLiteJni.predictImageSegmentNew(this.i, bitmap, a2);
        long checkpoint = timeRecorderNew.checkpoint(a2.getLong("preprocessEndTime"));
        Log.i("InferManager", "[stat]preprocess time: " + checkpoint);
        long end = timeRecorderNew.end();
        Log.i("InferManager", "[stat]forward time: " + end);
        String[] labels = this.h.getLabels();
        for (SegmentationResultModel segmentationResultModel : predictImageSegmentNew) {
            int labelIndex = segmentationResultModel.getLabelIndex();
            segmentationResultModel.setLabel((labelIndex < 0 || labelIndex >= labels.length) ? "UNKNOWN:" + labelIndex : labels[labelIndex]);
        }
        Log.i("InferManager segment", "segment result size " + predictImageSegmentNew.size());
        if (eVar != null) {
            eVar.setPreprocessTime(checkpoint);
            eVar.setForwardTime(end);
            eVar.setResultModel(predictImageSegmentNew);
        }
        c();
        return predictImageSegmentNew;
    }

    @Override // com.baidu.ai.edge.core.ocr.OcrInterface
    public List<OcrResultModel> ocr(Bitmap bitmap) throws BaseException {
        return ocr(bitmap, this.h.getRecommendedConfidence());
    }

    @Override // com.baidu.ai.edge.core.ocr.OcrInterface
    public List<OcrResultModel> ocr(Bitmap bitmap, float f) throws BaseException {
        return ocrInternal(bitmap, f, null);
    }

    public List<OcrResultModel> ocrInternal(Bitmap bitmap, float f, IStatisticsResultModel iStatisticsResultModel) throws BaseException {
        a();
        TimeRecorderNew timeRecorderNew = new TimeRecorderNew();
        JniParam a2 = a(bitmap, 100, f, new int[2]);
        float[] predictImageOcrNew = InferLiteJni.predictImageOcrNew(this.i, bitmap, a2);
        long checkpoint = timeRecorderNew.checkpoint(a2.getLong("preprocessEndTime"));
        Log.i("InferManager", "[stat]preprocess time: " + checkpoint);
        long checkpoint2 = timeRecorderNew.checkpoint();
        Log.i("InferManager", "[stat]forward time: " + checkpoint2);
        ArrayList<OcrResultModel> a3 = a(predictImageOcrNew, f);
        long end = timeRecorderNew.end();
        Log.i("InferManager", "[stat] ocr postprocess time:" + end);
        if (iStatisticsResultModel != null) {
            iStatisticsResultModel.setPreprocessTime(checkpoint);
            iStatisticsResultModel.setForwardTime(checkpoint2);
            iStatisticsResultModel.setPostprocessTime(end);
            iStatisticsResultModel.setResultModel(a3);
        }
        c();
        return a3;
    }

    @Override // com.baidu.ai.edge.core.pose.PoseInterface
    public List<PoseResultModel> pose(Bitmap bitmap) throws BaseException {
        return a(bitmap, 0.0f);
    }

    protected List<PoseResultModel> a(Bitmap bitmap, float f) throws BaseException {
        return b(bitmap, f, (IStatisticsResultModel) null);
    }

    @Override // com.baidu.ai.edge.core.pose.PoseInterface
    public StatisticsResultModelImpl posePro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        b(bitmap, this.h.getRecommendedConfidence(), (IStatisticsResultModel) eVar);
        return eVar;
    }

    @Override // com.baidu.ai.edge.core.segment.SegmentInterface
    public StatisticsResultModelImpl segmentPro(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        segmentInternal(bitmap, this.h.getRecommendedConfidence(), eVar);
        return eVar;
    }

    public List<OcrResultModel> ocrInternalOld(Bitmap bitmap, float f, IStatisticsResultModel iStatisticsResultModel) throws BaseException {
        a();
        TimerRecorder.start();
        Bitmap resizeWithStep = ImageUtil.resizeWithStep(bitmap, this.h.getMaxSize(), 32);
        float[] pixels = InferLiteJni.getPixels(resizeWithStep, this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), 0);
        long end = TimerRecorder.end();
        Log.i("InferManager", "[stat] ocr preprocess time:" + end);
        TimerRecorder.start();
        float[] fArr = {1.0f, 3.0f, resizeWithStep.getHeight(), resizeWithStep.getWidth()};
        Log.i("pixel size", "" + pixels.length + " height " + resizeWithStep.getHeight() + " ;width " + resizeWithStep.getWidth());
        int pixel = bitmap.getPixel(bitmap.getWidth() - 1, bitmap.getHeight() - 1);
        Log.i("Predictor", "pixels " + pixel + " " + Color.red(pixel) + " " + Color.blue(pixel) + " " + Color.green(pixel));
        float[] predictImageOcr = InferLiteJni.predictImageOcr(this.i, pixels, fArr, bitmap);
        long end2 = TimerRecorder.end();
        TimerRecorder.start();
        Log.i("InferManager", "[stat] ocr forward time:" + end2);
        ArrayList<OcrResultModel> a2 = a(predictImageOcr, f);
        long end3 = TimerRecorder.end();
        Log.i("InferManager", "[stat] ocr postprocess time:" + end3);
        if (iStatisticsResultModel != null) {
            iStatisticsResultModel.setPreprocessTime(end);
            iStatisticsResultModel.setForwardTime(end2);
            iStatisticsResultModel.setPostprocessTime(end3);
            iStatisticsResultModel.setResultModel(a2);
        }
        c();
        return a2;
    }

    public List<DetectionResultModel> detectOld(Bitmap bitmap, float f) throws BaseException {
        return a(bitmap, f, (IStatisticsResultModel) null);
    }

    public IStatisticsResultModel detectProOld(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        a(bitmap, this.h.getRecommendedConfidence(), (IStatisticsResultModel) eVar);
        return eVar;
    }

    public List<ClassificationResultModel> classifyOld(Bitmap bitmap, float f) throws BaseException {
        return c(bitmap, f, null);
    }

    public StatisticsResultModelImpl classifyProOld(Bitmap bitmap) throws BaseException {
        StatisticsResultModelImpl eVar = new StatisticsResultModelImpl();
        c(bitmap, this.h.getRecommendedConfidence(), eVar);
        return eVar;
    }

    public List<SegmentationResultModel> segmentOld(Bitmap bitmap, float f) throws BaseException {
        return segmentInternalOld(bitmap, f, null);
    }

    public List<SegmentationResultModel> segmentInternalOld(Bitmap bitmap, float f, StatisticsResultModelImpl eVar) throws BaseException {
        Pair<Bitmap, Float> resizeTarget = ImageUtil.resizeTarget(bitmap, this.h.getTargetSize(), this.h.getMaxSize());
        Bitmap bitmap2 = (Bitmap) resizeTarget.first;
        float floatValue = ((Float) resizeTarget.second).floatValue();
        float[] fArr = {1.0f, 3.0f, bitmap2.getHeight(), bitmap2.getWidth()};
        TimerRecorder.start();
        float[] pixels = InferLiteJni.getPixels(ImageUtil.resize(bitmap2, bitmap2.getWidth(), bitmap2.getHeight()), this.h.getImgMeans(), this.h.getScales(), this.h.isHWC(), this.h.isRGB(), this.h.getNType() == 2010 ? 32 : 0);
        long end = TimerRecorder.end();
        Log.i("InferManager segment", "[stat] preprocess time:" + end);
        Log.i("InferManager segment", "pixels：" + pixels.length);
        TimerRecorder.start();
        ArrayList<SegmentationResultModel> predictImageSegment = InferLiteJni.predictImageSegment(this.i, pixels, fArr, floatValue, f);
        long end2 = TimerRecorder.end();
        Log.i("InferManager segment", "[stat] forward time:" + end2);
        TimerRecorder.start();
        String[] labels = this.h.getLabels();
        for (SegmentationResultModel segmentationResultModel : predictImageSegment) {
            segmentationResultModel.setLabel(labels[segmentationResultModel.getLabelIndex()]);
        }
        long end3 = TimerRecorder.end();
        Log.i("InferManager segment", "segment result size " + predictImageSegment.size());
        if (eVar != null) {
            eVar.setPreprocessTime(end);
            eVar.setForwardTime(end2);
            eVar.setPostprocessTime(end3);
            eVar.setResultModel(predictImageSegment);
        }
        c();
        return predictImageSegment;
    }

    public List<OcrResultModel> ocrOld(Bitmap bitmap, float f) throws BaseException {
        return ocrInternalOld(bitmap, f, null);
    }
}
