package com.baidu.ai.edge.core.base;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/base/BaseResultModel.class */
public class BaseResultModel implements Comparable<BaseResultModel>, IBaseResultModel {

    /* renamed from: a  reason: collision with root package name */
    protected int f11a;
    protected String b;
    protected float c;

    public BaseResultModel() {
    }

    public BaseResultModel(String str, float f) {
        this.b = str;
        this.c = f;
    }

    public BaseResultModel(int i, float f) {
        this.f11a = i;
        this.c = f;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public int getLabelIndex() {
        return this.f11a;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public void setLabelIndex(int i) {
        this.f11a = i;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public String getLabel() {
        return this.b;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public void setLabel(String str) {
        this.b = str;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public float getConfidence() {
        return this.c;
    }

    @Override // com.baidu.ai.edge.core.base.IBaseResultModel
    public void setConfidence(float f) {
        this.c = f;
    }

    public JSONObject toJsonObject() {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("label", this.b);
            jSONObject.put("confidence", this.c);
        } catch (JSONException e) {
            Log.e("BaseResultModel", "json serialize error", e);
        }
        return jSONObject;
    }

    @Override // java.lang.Comparable
    public int compareTo(BaseResultModel baseResultModel) {
        return Float.valueOf(baseResultModel.getConfidence()).compareTo(Float.valueOf(this.c));
    }
}
