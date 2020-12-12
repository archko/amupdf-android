package com.artifex.solib.animation;

public class SOAnimationColourEffectCommand extends SOAnimationRunningCommand
{
    public int   effect; // an effect. see SOAnimationColourEffects

    public SOAnimationColourEffectCommand(int     layer,
                                          int     turns,
                                          boolean reversed,
                                          boolean bouncing,
                                          float   delay,
                                          float   duration,
                                          int     effect) {
        super(layer, turns, reversed, bouncing, delay, duration);
        this.effect = effect;
    }

    public String toString() {
        return String.format("SOAnimationColourEffectCommand(%s %d)",
                super.toString(),
                effect);
    }
}
