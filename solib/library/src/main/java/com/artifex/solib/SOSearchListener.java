package com.artifex.solib;

import android.graphics.RectF;

public interface SOSearchListener
{
    /* Interface to keep abreast of search events */

    /**
     * Called to indicate that the search has progressed to a new page.
     *
     * May be called multiple times during a search.
     *
     * @param page  current page being searched.
     */
    void progressing(int page);

    /**
     * Called when the search terminates successfully.
     *
     * No further callbacks will be made.
     *
     * @param page  page on which match found
     * @param box   bounding box of match
     */
    void found(int page, RectF box);

    /**
     * Called when the search terminates unsuccessfully.
     *
     * No further callbacks will be made.
     */
    void notFound();

    /**
     * Called when the search reaches the start of the document.
     *
     * @return false to stop the search here - no further callbacks will be made. Return true
     *         to continue searching.
     */
    boolean startOfDocument();

    /**
     * Called when the search reaches the end of the document.
     *
     * @return false to stop the search here - no further callbacks will be made. Return true
     *         to continue searching.
     */
    boolean endOfDocument();

    /**
     * Called when the search encounters an error.
     *
     * No further callbacks will be made.
     */
    void error();

    /**
     * Called if  the search is cancelled.
     *
     * No further callbacks will be made.
     */
    void cancelled();
}
