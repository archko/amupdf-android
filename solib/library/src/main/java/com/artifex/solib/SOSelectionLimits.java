package com.artifex.solib;

import android.graphics.PointF;
import android.graphics.RectF;

public class SOSelectionLimits extends ArDkSelectionLimits
{
    /* Information as to the type and position of a selection */

    /**
     * Read the start of the selected area.
     *
     * @return Point representing the start of the selection.
     */
    public native PointF getStart();

    /**
     * Read the end of the selected area.
     *
     * @return Point representing the end of the selection.
     */
    public native PointF getEnd();

    /**
     * Read the position where the handle should be attached to the selection.
     *
     * @return Point where the handle should be attached to the selection.
     */
    public native PointF getHandle();

    /**
     * Read the bounding box of the selection.
     *
     * @return Selection bounding box.
     */
    public native RectF getBox();

    /**
     * Determine if the Selection has a well defined start.
     *
     * @return true if the selection has a well defined start value.
     */
    public native boolean getHasSelectionStart();

    /**
     * Determine if the Selection has a well defined end.
     *
     * @return true if the selection has a well defined start end.
     */
    public native boolean getHasSelectionEnd();

    /**
     * Determine if the selection is Active.
     *
     * @return true if the selection is active.
     */
    public native boolean getIsActive();

    /**
     * Determine if the selection is a Caret.
     *
     * @return true if the selection is a caret.
     */
    public native boolean getIsCaret();

    /**
     * Determine if the selection is extensible.
     *
     * @return true if the selection is extensible.
     */
    public native boolean getIsExtensible();

    /**
     * Determine if the selection has pending visual changes.
     *
     * @return true if the selection has pending visual changes.
     */
    public native boolean getHasPendingVisualChanges();

    /**
     * Determine if the selection is an IME composition.
     *
     * @return true if the selection is an IME composition.
     */
    public native boolean getIsComposing();

    /**
     * Scale the positional information.
     *
     * @param scale  factor to scale by.
     */
    public native void scaleBy(double scale);

    /**
     * Offset the positional information.
     *
     * @param offset   vector to offset by.
     */
    public void offsetBy(PointF offset)
    {
        offsetBy(offset.x, offset.y);
    }

    /**
     * Offset the positional information.
     *
     * @param offX   x value to offset by.
     * @param offY   y value to offset by.
     */
    public native void offsetBy(double offX, double offY);

    /**
     * Combine another SOSelectionLimits with this one.
     *
     * Typically this is used to combine multiple records for different pages.
     *
     * @note limits must be from a later point in the document than 'this'.
     *
     * @param limits   SOSelectionLimits to combine with this one.
     */
    public void combine(ArDkSelectionLimits limits)
    {
        combineWith((SOSelectionLimits)limits);
    }
    public native void combineWith(SOSelectionLimits limits);

    /* Private implementation details */

    /* C struct pointer cast to a long */
    private long internal;

    /**
     * Internal constructor, only ever called from JNI
     *
     * @param internal   C struct pointer cast to a long.
     */
    protected SOSelectionLimits(long internal)
    {
        this.internal = internal;
    }

    protected SOSelectionLimits()
    {
        //  this is a default ctor that does nothing, so
        //  MuPDFSelectionLimits won't have anything to do with SO jni.
    }

    /**
     * Private method to destroy the underlying C structure, used by the finalizer.
     */
    private native void destroy();

    /**
     * Finalizer to free the underlying C structure when gc'd.
     */
    protected void finalize() throws Throwable
    {
        try
        {
            destroy();
        }
        finally
        {
            super.finalize();
        }
    }
}
