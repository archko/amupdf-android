package com.baidu.ai.edge.ui.view.model;

import android.graphics.Rect;

/**
 * Created by ruanshimin on 2018/5/13.
 */

public class DetectResultModel extends BasePolygonResultModel {
    public DetectResultModel() {
        super();
    }

    public DetectResultModel(int index, String name, float confidence, Rect bounds) {
        super(index, name, confidence, bounds);

    }
}
