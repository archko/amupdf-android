package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationRenderCommand extends SOAnimationCommand {
    public int    renderable;
    public float  zoom;
    public PointF origin;
    public float  x, y, w, h;

    public SOAnimationRenderCommand(int    layer,
                                    int    renderable,
                                    float  zoom,
                                    PointF origin,
                                    float  x,
                                    float  y,
                                    float  w,
                                    float  h) {
        super(layer);
        this.renderable = renderable;
        this.zoom       = zoom;
        this.origin     = origin;
        this.x          = x;
        this.y          = y;
        this.w          = w;
        this.h          = h;
    }

    public String toString() {
        return String.format("SOAnimationRenderCommand(%s, %d, %.2f, (%.2f, %.2f), (%.2f, %.2f, %.2f, %.2f))",
                super.toString(),
                renderable,
                zoom,
                origin.x, origin.y,
                x, y, w, h);
    }
}
