package com.artifex.solib;

import android.graphics.Point;
import android.graphics.PointF;

public class ArDkPage {

    /* Render the rearmost static page content */
    public static final int SOLayer_Background = -1;

    /* Render the default view of page content */
    public static final int SOLayer_All        = -2;

    /* Specify this when no layer is required or appropriate */
    public static final int SOLayer_None       = -3;

    /* Selection modes for select method: */

    /* Reposition start of selection region */
    public static final int SOSelectMode_Start       = 0;

    /* Reposition end of selection region */
    public static final int SOSelectMode_End         = 1;

    /* Select nearest word */
    public static final int SOSelectMode_DefaultUnit = 2;

    /* Place a caret */
    public static final int SOSelectMode_Caret       = 3;

    public PointF zoomToFitRect(int w, int h) {return null;};
    public Point sizeAtZoom(double zoom) {return null;};

    public ArDkRender renderLayerAtZoomWithAlpha (
            final int layer,
            final double zoom,
            final double originX,
            final double originY,
            final ArDkBitmap bitmap,
            final ArDkBitmap alpha,
            final SORenderListener listener,
            final boolean uiThread,
            final boolean inverted) {return null;}

    // Shim
    public ArDkRender renderAtZoom(double           zoom,
                                   PointF           origin,
                                   ArDkBitmap         bitmap,
                                   SORenderListener listener,
                                   boolean inverted)
    {
        return renderLayerAtZoomWithAlpha(SOLayer_All,
                zoom,
                origin.x, origin.y,
                bitmap,
                null,
                listener,
                true, inverted);
    }

    public int select(int mode, double atX, double atY) {return 0;};
    public ArDkSelectionLimits selectionLimits() {return null;};
    public void releasePage() {};
    public void destroyPage() {};
    public SOHyperlink objectAtPoint(float atX, float atY) {return null;}
}
