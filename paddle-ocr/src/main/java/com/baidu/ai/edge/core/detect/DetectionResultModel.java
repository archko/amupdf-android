package com.baidu.ai.edge.core.detect;

import android.graphics.Rect;
import android.util.Log;
import com.baidu.ai.edge.core.base.BaseResultModel;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/detect/DetectionResultModel.class */
public class DetectionResultModel extends BaseResultModel implements IDetectionResultModel {
    private Rect d;

    public DetectionResultModel() {
    }

    public DetectionResultModel(String str, float f, Rect rect) {
        super(str, f);
        this.d = rect;
    }

    @Override // com.baidu.ai.edge.core.detect.IDetectionResultModel
    public void setBounds(Rect rect) {
        this.d = rect;
    }

    @Override // com.baidu.ai.edge.core.detect.IDetectionResultModel
    public Rect getBounds() {
        return this.d;
    }

    public String toString() {
        return "[" + this.b + "][" + this.c + "][" + this.d.toString() + "]";
    }

    @Override // com.baidu.ai.edge.core.base.BaseResultModel
    public JSONObject toJsonObject() {
        JSONObject jsonObject = super.toJsonObject();
        try {
            jsonObject.put("bounds_left", this.d.left);
            jsonObject.put("bounds_top", this.d.top);
            jsonObject.put("bounds_right", this.d.right);
            jsonObject.put("bounds_bottom", this.d.bottom);
        } catch (JSONException e) {
            Log.e("DetectionResultModel", "json serialize error", e);
        }
        return jsonObject;
    }
}
