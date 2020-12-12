package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationRotateCommand extends SOAnimationRunningCommand {
    public PointF origin;
    public float  startAngle, endAngle;
    public int    profile; // an easing profile. see SOAnimationEasings

    public SOAnimationRotateCommand(int     layer,
                                    int     turns,
                                    boolean reversed,
                                    boolean bouncing,
                                    float   delay,
                                    float   duration,
                                    PointF  origin,
                                    float   startAngle,
                                    float   endAngle,
                                    int     profile) {
        super(layer, turns, reversed, bouncing, delay, duration);
        this.origin     = origin;
        this.startAngle = startAngle;
        this.endAngle   = endAngle;
        this.profile    = profile;
    }

    public String toString() {
        return String.format("SOAnimationRotateCommand(%s (%.2f, %.2f), %.2f, %.2f, %d)",
                super.toString(),
                origin.x, origin.y,
                startAngle, endAngle,
                profile);
    }
}
