package com.baidu.ai.edge.ui.view.model;

/**
 * Created by ruanshimin on 2018/5/16.
 */

public class BaseResultModel {
    private int index;
    private String name;
    private float confidence;

    protected BaseResultModel() {

    }

    protected BaseResultModel(int index, String name, float confidence) {
        this.index = index;
        this.name = name;
        this.confidence = confidence;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
