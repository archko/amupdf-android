package com.artifex.solib.animation;

/**
 * Abstract base class for SmartOffice animation commands.
 */
public abstract class SOAnimationCommand {
    public int layer;

    SOAnimationCommand(int layer) {
        this.layer = layer;
    }

    public String toString() {
        return String.format("SOAnimationCommand(%d)", layer);
    }
}
