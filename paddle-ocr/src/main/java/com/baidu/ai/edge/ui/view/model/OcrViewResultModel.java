package com.baidu.ai.edge.ui.view.model;

import android.graphics.Point;

import java.util.List;

public class OcrViewResultModel extends BasePolygonResultModel {
    public OcrViewResultModel() {
        super();
    }

    public OcrViewResultModel(int index, String name, float confidence, List<Point> bounds) {
        super(index, name, confidence, bounds);
    }
}
