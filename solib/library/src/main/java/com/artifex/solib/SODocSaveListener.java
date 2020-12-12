package com.artifex.solib;

public interface SODocSaveListener
{
    /* Interface for a listener to stay abreast of events as documents are saved. */

    int SODocSave_Succeeded = 0;
    int SODocSave_Error = 1;
    int SODocSave_Cancelled = 2;

    /**
     * Called on completion of the save process.
     *
     * @param result   success/failure code.
     * @param err      SOL success/failure code.
     */
    void onComplete(int result, int err);
}
