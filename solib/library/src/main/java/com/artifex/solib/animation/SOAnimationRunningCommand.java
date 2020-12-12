package com.artifex.solib.animation;

/**
 * Abstract base class for commands which run for a period of time.
 */
public abstract class SOAnimationRunningCommand extends SOAnimationCommand {
    // Looping
    public int     turns;
    public boolean reversed;
    public boolean bouncing;

    // Timing
    public float   delay;
    public float   duration;

    public SOAnimationRunningCommand(int     layer,
                                     int     turns,
                                     boolean reversed,
                                     boolean bouncing,
                                     float   delay,
                                     float   duration) {
        super(layer);
        this.turns    = turns;
        this.reversed = reversed;
        this.bouncing = bouncing;
        this.delay    = delay;
        this.duration = duration;
    }

    public String toString() {
        return String.format("SOAnimationRunningCommand(%d %d %b %b %.2f %.2f)",
                layer,
                turns,
                reversed,
                bouncing,
                delay,
                duration);
    }
}
