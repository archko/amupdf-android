package com.artifex.solib.animation;

public class SOAnimationSetTransformCommand extends SOAnimationImmediateCommand {
    public float trfmA, trfmB, trfmC, trfmD, trfmX, trfmY;

    public SOAnimationSetTransformCommand(int   layer,
                                          float delay,
                                          float trfmA,
                                          float trfmB,
                                          float trfmC,
                                          float trfmD,
                                          float trfmX,
                                          float trfmY) {
        super(layer, delay);
        this.trfmA = trfmA;
        this.trfmB = trfmB;
        this.trfmC = trfmC;
        this.trfmD = trfmD;
        this.trfmX = trfmX;
        this.trfmY = trfmY;
    }

    public String toString() {
        return String.format("SOAnimationSetTransformCommand(%s (%.2f %.2f %.2f %.2f %.2f %.2f)",
                super.toString(),
                trfmA, trfmB, trfmC, trfmD, trfmX, trfmY);
    }
}
