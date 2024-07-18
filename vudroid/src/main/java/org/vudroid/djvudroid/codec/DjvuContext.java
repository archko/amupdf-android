package org.vudroid.djvudroid.codec;

import android.content.ContentResolver;

import org.vudroid.core.VuDroidLibraryLoader;
import org.vudroid.core.codec.CodecContext;

import java.util.concurrent.Semaphore;

public class DjvuContext implements CodecContext {
    static {
        VuDroidLibraryLoader.load();
    }

    private long contextHandle;
    private static final String DJVU_DROID_CODEC_LIBRARY = "DjvuDroidCodecLibrary";
    private final Object waitObject = new Object();
    private final Semaphore docSemaphore = new Semaphore(0);

    public DjvuContext() {
        this.contextHandle = create();
    }

    public DjvuDocument openDocument(String fileName) {
        final DjvuDocument djvuDocument = DjvuDocument.openDocument(fileName, this, waitObject);
        return djvuDocument;
    }

    long getContextHandle() {
        return contextHandle;
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (isRecycled()) {
            return;
        }
        free(contextHandle);
        contextHandle = 0;
    }

    private boolean isRecycled() {
        return contextHandle == 0;
    }

    private static native long create();

    private static native void free(long contextHandle);

    private native void handleMessage(long contextHandle);
}
