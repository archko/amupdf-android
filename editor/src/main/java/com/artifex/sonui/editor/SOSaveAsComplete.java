package com.artifex.sonui.editor;

public interface SOSaveAsComplete
{
    // Interface to convey the success or otherwise of a SaveAs operation.

     /**
      * The operation was successful
      */
    int SOSaveAsComplete_Succeeded = 0;

     /**
      * The operation failed.
      */
    int SOSaveAsComplete_Error     = 1;

     /**
      * The operation was cancelled.
      */
    int SOSaveAsComplete_Cancelled = 2;


    /**
     * Called on completion of the filename selection process.
     *
     * @param path     desired path for saved file
     * @return true to continue, false to abort save
     */
    boolean onFilenameSelected(String path);

    /**
     * Called on completion of the saveAs process.
     *
     * @param result   success/failure code.
     * @param path     path to saved file
     */
    void onComplete(int result, String path);
}

