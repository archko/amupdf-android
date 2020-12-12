package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationScaleCommand extends SOAnimationRunningCommand {
    public float  startX, startY; // A pair of scales, not a PointF
    public float  endX, endY;
    public PointF centre;
    public int    profile; // an easing profile. see SOAnimationEasings

    public SOAnimationScaleCommand(int     layer,
                                   int     turns,
                                   boolean reversed,
                                   boolean bouncing,
                                   float   delay,
                                   float   duration,
                                   float   startX,
                                   float   startY,
                                   float   endX,
                                   float   endY,
                                   PointF  centre,
                                   int     profile) {
        super(layer, turns, reversed, bouncing, delay, duration);
        this.startX  = startX;
        this.startY  = startY;
        this.endX    = endX;
        this.endY    = endY;
        this.centre  = centre;
        this.profile = profile;
    }

    public String toString() {
        return String.format("SOAnimationScaleCommand(%s (%.2f, %.2f) (%.2f, %.2f) (%.2f, %.2f) %d)",
                super.toString(),
                startX, startY,
                endX, endY,
                centre.x, centre.y,
                profile);
    }
}
