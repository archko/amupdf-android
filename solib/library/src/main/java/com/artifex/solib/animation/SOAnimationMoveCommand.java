package com.artifex.solib.animation;

import android.graphics.PointF;

public class SOAnimationMoveCommand extends SOAnimationRunningCommand {
    public PointF start, end;
    public int    profile; // an easing profile. see SOAnimationEasings

    public SOAnimationMoveCommand(int     layer,
                                  int     turns,
                                  boolean reversed,
                                  boolean bouncing,
                                  float   delay,
                                  float   duration,
                                  PointF  start,
                                  PointF  end,
                                  int     profile) {
        super(layer, turns, reversed, bouncing, delay, duration);
        this.start   = start;
        this.end     = end;
        this.profile = profile;
    }

    public String toString() {
        return String.format("SOAnimationMoveCommand(%s (%.2f, %.2f) (%.2f, %.2f) %d)",
                super.toString(),
                start.x, start.y,
                end.x, end.y,
                profile);
    }
}
