package com.artifex.solib.animation;

public class SOAnimationWaitForLayerCommand extends SOAnimationCommand {
    public int waitee; // a layer
    public int whence;

    public SOAnimationWaitForLayerCommand(int layer, int waitee, int whence) {
        super(layer);
        this.waitee = waitee;
        this.whence = whence;
    }

    public String toString() {
        return String.format("SOAnimationWaitForLayerCommand(%s %d %d)", super.toString(), waitee, whence);
    }
}
