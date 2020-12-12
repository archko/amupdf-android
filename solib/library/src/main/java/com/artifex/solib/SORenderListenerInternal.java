package com.artifex.solib;

interface SORenderListenerInternal
{
    /* Interface to keep abreast of render completion. */

    /**
     * Called strictly once when a render completes.
     *
     * @param error  error code - 0 for no error.
     */
    void progress(int error);
}
