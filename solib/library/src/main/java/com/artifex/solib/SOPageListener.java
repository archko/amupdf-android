package com.artifex.solib;

import android.graphics.RectF;

public interface SOPageListener
{
    /* Interface to listen to page update events */

    /**
     * Called when an area of the page has been updated.
     *
     * For example, when a selection is made, the page needs to be redrawn so that the
     * selection is visually shown. Similarly when edits are made.
     *
     * In these cases, SmartOffice calls the update function, and relies on the app requesting
     * redraws as required.
     *
     * @param area  The area of the page to be updated.
     */
    void update(RectF area);
}
