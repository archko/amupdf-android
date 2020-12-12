package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationSetPositionCommand extends SOAnimationImmediateCommand {
    public PointF newOrigin;

    public SOAnimationSetPositionCommand(int layer, float delay, PointF newOrigin) {
        super(layer, delay);
        this.newOrigin = newOrigin;
    }

    public String toString() {
        return String.format("SOAnimationSetPositionCommand(%s (%.2f %.2f))",
                super.toString(),
                newOrigin.x, newOrigin.y);
    }
}
