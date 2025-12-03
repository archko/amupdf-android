package org.vudroid.djvudroid.codec;

import android.content.ContentResolver;

import org.vudroid.core.codec.CodecContext;

public class DjvuContext implements CodecContext {

    public DjvuContext() {
    }

    public DjvuDocument openDocument(String fileName) {
        final DjvuDocument djvuDocument = DjvuDocument.openDocument(fileName, this);
        return djvuDocument;
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        // No JNI to free
    }
}
