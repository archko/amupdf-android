package com.artifex.solib;

public interface SODocLoadListener
{
    /* Interface for a listener to stay abreast of events as documents are loaded. */

    /**
     * Called after each page is loaded.
     *
     * @param pageNum  number of page that has just been loaded (0 <= pageNum < numPages-1)
     */
    void onPageLoad (int pageNum);

    /**
     * Called when the document completes loading.
     *
     * Will be called strictly once, whether the document completes correctly or not.
     */
    void onDocComplete();

    /**
     * Called if the document encounters an error.
     *
     * @param error     error code.
     * @param errorNum  underlying core error number
     */
    void onError(int error, int errorNum);

    //  monitor selection change
    void onSelectionChanged(int startPage, int endPage);

    //  monitor core layout changes
    void onLayoutCompleted();
}
