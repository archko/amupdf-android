package com.artifex.solib.animation;

public class SOAnimationWaitForEventCommand extends SOAnimationCommand {
    public int event; // an event. see SOAnimationEvents

    public SOAnimationWaitForEventCommand(int layer, int event) {
        super(layer);
        this.event = event;
    }

    public String toString() {
        return String.format("SOAnimationWaitForEventCommand(%s %d)", super.toString(), event);
    }
}
