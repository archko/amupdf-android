package com.artifex.solib.animation;

public class SOAnimationDisposeCommand extends SOAnimationCommand {
    SOAnimationDisposeCommand(int layer) {
        super(layer);
    }

    public String toString() {
        return String.format("SOAnimationDisposeCommand(%s)", super.toString());
    }
}
