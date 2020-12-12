package com.artifex.solib;

public class SOSelectionTableRange
{
    public native int firstColumn();
    public native int columnCount();
    public native int firstRow();
    public native int rowCount();

    /* Private implementation details */

    /* C struct pointer cast to a long */
    private long internal;

    /**
     * Internal constructor, only ever called from JNI
     *
     * @param internal   C struct pointer cast to a long.
     */
    private SOSelectionTableRange(long internal)
    {
        this.internal = internal;
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
