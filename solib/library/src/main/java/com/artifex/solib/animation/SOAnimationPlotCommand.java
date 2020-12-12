package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationPlotCommand extends SOAnimationImmediateCommand {
    public PointF position;
    public int    zPosition;

    public SOAnimationPlotCommand(int layer, float delay, PointF position, int zPosition) {
        super(layer, delay);
        this.position  = position;
        this.zPosition = zPosition;
    }

    public String toString() {
        return String.format("SOAnimationPlotCommand(%s (%.2f, %.2f) %d)",
                super.toString(),
                position.x, position.y,
                zPosition);
    }
}
