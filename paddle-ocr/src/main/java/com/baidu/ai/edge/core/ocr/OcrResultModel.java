package com.baidu.ai.edge.core.ocr;

import android.graphics.Point;
import com.baidu.ai.edge.core.base.BaseResultModel;
import java.util.ArrayList;
import java.util.List;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/ocr/OcrResultModel.class */
public class OcrResultModel extends BaseResultModel {
    private List<Point> d = new ArrayList();
    private List<Integer> e = new ArrayList();

    public void addPoints(int i, int i2) {
        this.d.add(new Point(i, i2));
    }

    public void addWordIndex(int i) {
        this.e.add(Integer.valueOf(i));
    }

    public List<Point> getPoints() {
        return this.d;
    }

    public List<Integer> getWordIndex() {
        return this.e;
    }
}
