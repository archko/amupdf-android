package com.artifex.solib.animation;

public class SOAnimationWaitForTimeCommand extends SOAnimationCommand {
    public float delay;

    public SOAnimationWaitForTimeCommand(int layer, float delay) {
        super(layer);
        this.delay = delay;
    }

    public String toString() {
        return String.format("SOAnimationWaitForTimeCommand(%s %.2f)", super.toString(), delay);
    }
}
