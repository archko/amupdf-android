package com.artifex.solib;

public interface SODocLoadListenerInternal
{
    /* Internal interface. */

    /**
     * Called after every successful page load, and again on completion.
     *
     * @param numPages  numPages loaded so far
     * @param complete  true if the document load has completed, false otherwise.
     */
    void progress(int numPages, boolean complete);

    /**
     * Called on an error.
     *
     * @param error     error code.
     * @param errorNum  underlying core error number
     */
    void error(int error, int errorNum);

    //  monitor selection change
    void onSelectionChanged(int startPage, int endPage);

    //  monitor core layout changes
    void onLayoutCompleted();

    //  store the document to which this listener applies.
    void setDoc(ArDkDoc doc);
}
