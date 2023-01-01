package com.baidu.ai.edge.ui.view.model;

public class PoseViewResultModel extends BasePolygonResultModel {
    public PoseViewResultModel() {
        super();
        setRect(false);
        setTextOverlay(false);
        setDrawPoints(true);
        setMultiplePairs(true);
    }
}
