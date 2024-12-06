package org.vudroid.core.models;

import org.vudroid.core.events.BringUpZoomControlsEvent;
import org.vudroid.core.events.EventDispatcher;
import org.vudroid.core.events.ZoomChangedEvent;
import org.vudroid.core.events.ZoomListener;

public class ZoomModel extends EventDispatcher {
    private float zoom = 1.0f;
    private static final float INCREMENT_DELTA = 0.05f;
    private boolean horizontalScrollEnabled;
    private boolean isCommited;
    private float MAX_ZOOM = 12.0f;

    public void setZoom(float zoom) {
        zoom = Math.min(Math.max(zoom, 1.0f), MAX_ZOOM);
        if (this.zoom != zoom) {
            float oldZoom = this.zoom;
            this.zoom = zoom;
            isCommited = false;
            dispatch(new ZoomChangedEvent(zoom, oldZoom));
        }
    }

    public void setMaxZoom(float maxZoom) {
        this.MAX_ZOOM = maxZoom;
    }

    public float getZoom() {
        return zoom;
    }

    public void increaseZoom() {
        setZoom(getZoom() + INCREMENT_DELTA);
    }

    public void decreaseZoom() {
        setZoom(getZoom() - INCREMENT_DELTA);
    }

    public void toggleZoomControls() {
        dispatch(new BringUpZoomControlsEvent());
    }

    public void setHorizontalScrollEnabled(boolean horizontalScrollEnabled) {
        this.horizontalScrollEnabled = horizontalScrollEnabled;
    }

    public boolean isHorizontalScrollEnabled() {
        return horizontalScrollEnabled;
    }

    public boolean canDecrement() {
        return zoom > 1.0f;
    }

    public void commit() {
        if (!isCommited) {
            isCommited = true;
            dispatch(new ZoomListener.CommitZoomEvent());
        }
    }
}
