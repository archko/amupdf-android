package com.artifex.solib.animation;

/**
 * Abstract base class for commands which have an immediate effect.
 */
public abstract class SOAnimationImmediateCommand extends SOAnimationCommand {
    public float delay;

    SOAnimationImmediateCommand(int layer, float delay) {
        super(layer);
        this.delay = delay;
    }

    public String toString() {
        return String.format("SOAnimationImmediateCommand(%s %.2f)", super.toString(), delay);
    }
}
