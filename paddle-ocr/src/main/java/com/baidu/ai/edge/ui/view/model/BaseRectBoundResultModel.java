package com.baidu.ai.edge.ui.view.model;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by ruanshimin on 2018/5/16.
 */

public class BaseRectBoundResultModel extends BaseResultModel {

    private int colorId;

    public int getColorId() {
        return colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    private byte[] mask;

    BaseRectBoundResultModel() {
        super();
    }

    BaseRectBoundResultModel(int index, String name, float confidence, Rect bounds) {
        super(index, name, confidence);
        this.bounds = bounds;
    }

    public byte[] getMask() {
        return mask;
    }

    public void setMask(byte[] mask) {
        this.mask = mask;
    }

    private Rect bounds;

    public Rect getBounds() {
        return bounds;
    }

    public Rect getBounds(float ratio, Point origin) {
        return new Rect((int) (origin.x + bounds.left * ratio),
                (int) (origin.y + bounds.top * ratio),
                (int) (origin.x + bounds.right * ratio),
                (int) (origin.y + bounds.bottom * ratio));
    }

    public void setBounds(Rect bounds) {
        this.bounds = bounds;
    }

    public boolean isHasMask() {
        return (mask != null);
    }

}
