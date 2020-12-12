package com.artifex.sonui.editor;

public interface SOCustomSaveComplete
{
    // Interface to convey the success or otherwise of a Custom Save operation.

     /**
      * The operation was successful
      */
    int SOCustomSaveComplete_Succeeded = 0;

     /**
      * The operation failed.
      */
    int SOCustomSaveComplete_Error     = 1;

     /**
      * The operation was cancelled.
      */
    int SOCustomComplete_Cancelled = 2;

    /**
     * Called on completion of the saveAs process.
     *
     * @param result        success/failure code.
     * @param path          path to saved file
     * @param closeDocument if true document should be closed
     */
    void onComplete(int result, String path, boolean closeDocument);
}

