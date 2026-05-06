package com.baidu.ai.edge.core.classify;

import com.baidu.ai.edge.core.base.BaseConfig;
import com.baidu.ai.edge.core.base.BaseManager;
import com.baidu.ai.edge.core.ddk.DDKManager;
import com.baidu.ai.edge.core.ddk.DavinciManager;
import com.baidu.ai.edge.core.detect.DetectPostProcess;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.snpe.SnpeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/classify/ClassifyPostProcess.class */
public class ClassifyPostProcess {

    /* renamed from: a  reason: collision with root package name */
    private BaseConfig f17a;
    private int b;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.baidu.ai.edge.core.classify.ClassifyPostProcess$ConfidenceComparator  reason: collision with other inner class name */
    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/classify/ClassifyPostProcess$ConfidenceComparator.class */
    public class ConfidenceComparator implements Comparator<ClassificationResultModel> {
        ConfidenceComparator(ClassifyPostProcess classifyPostProcess) {
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

    public ClassifyPostProcess(Class<? extends BaseManager> cls, BaseConfig baseConfig, int i, int i2, int i3, int i4) {
        this.b = 1;
        this.f17a = baseConfig;
        baseConfig.getNType();
        if (cls.equals(InferManager.class) || cls.equals(DavinciManager.class)) {
            this.b = 1;
        } else if (!cls.equals(DDKManager.class) && !cls.equals(SnpeManager.class)) {
            throw new RuntimeException("ClassifyPostProcess class not support " + cls.getSimpleName());
        }
    }

    private List<ClassificationResultModel> b(float[] fArr, float f) {
        ArrayList arrayList = new ArrayList();
        String[] labels = this.f17a.getLabels();
        int i = 0;
        while (i < fArr.length) {
            if (fArr[i] >= f) {
                ClassificationResultModel classificationResultModel = new ClassificationResultModel((i >= labels.length || i < 0) ? "UNKNOWN:" + i : labels[i], fArr[i]);
                classificationResultModel.setLabelIndex(i);
                arrayList.add(classificationResultModel);
            }
            i++;
        }
        Collections.sort(arrayList, new ConfidenceComparator(this));
        return arrayList;
    }

    public List<ClassificationResultModel> a(float[] fArr, float f) {
        if (this.b == 1) {
            return b(fArr, f);
        }
        return null;
    }
}
