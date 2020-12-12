package com.artifex.solib.animation;

public class SOAnimationFadeCommand extends SOAnimationRunningCommand {
    public int   effect; // an effect. see SOAnimationEffects
    public int   subType; //Effect sub-type. see SOAnimationEffectSubtype
    public float startOpacity, endOpacity;
    public int   profile; // an easing profile. see SOAnimationEasings

    public SOAnimationFadeCommand(int     layer,
                                  int     turns,
                                  boolean reversed,
                                  boolean bouncing,
                                  float   delay,
                                  float   duration,
                                  int     effect,
                                  int     subType,
                                  float   startOpacity,
                                  float   endOpacity,
                                  int     profile) {
        super(layer, turns, reversed, bouncing, delay, duration);
        this.effect       = effect;
        this.subType      = subType;
        this.startOpacity = startOpacity;
        this.endOpacity   = endOpacity;
        this.profile      = profile;
    }

    public String toString() {
        return String.format("SOAnimationFadeCommand(%s %d %.2f %.2f %d)",
                super.toString(),
                effect,
                startOpacity, endOpacity,
                profile);
    }
}
