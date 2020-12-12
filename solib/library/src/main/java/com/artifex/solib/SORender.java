package com.artifex.solib;

public class SORender extends ArDkRender
{
    /**
     * Abort a previously started render. Calling this for a render that
     * has yet to complete should avoid any delay when the system comes
     * to deallocate it. Otherwise dealloc may wait for the render to
     * complete, momentarily stalling the UI thread.
     */
    public native void abort();

    /**
     * Destroy
     */
    public native void destroy();

    /* Implementation details */

    /**
     * Private constructor. Called from the JNI.
     *
     * @param internal   C pointer cast to a long.
     */
    protected SORender(long internal)
    {
        this.internal = internal;
    }

    /* C pointer cast to a long */
    private long internal;

    /* No finalize required. The C holds a reference to the SORender object, so finalize
     * can never be called while a render is running. Therefore we'd have nothing to do. */
}
