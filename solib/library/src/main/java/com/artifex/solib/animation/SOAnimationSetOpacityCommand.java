package com.artifex.solib.animation;

public class SOAnimationSetOpacityCommand extends SOAnimationImmediateCommand {
    public float opacity;

    public SOAnimationSetOpacityCommand(int layer, float delay, float opacity) {
        super(layer, delay);
        this.opacity = opacity;
    }

    public String toString() {
        return String.format("SOAnimationSetOpacityCommand(%s %.2f)",
                super.toString(),
                opacity);
    }
}
