package com.artifex.solib.animation;

public class SOAnimationSetVisibilityCommand extends SOAnimationImmediateCommand {
    public boolean visible;

    public SOAnimationSetVisibilityCommand(int layer, float delay, boolean visible) {
        super(layer, delay);
        this.visible = visible;
    }

    public String toString() {
        return String.format("SOAnimationSetVisibilityCommand(%s %b)",
                super.toString(),
                visible);
    }
}
