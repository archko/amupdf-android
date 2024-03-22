package org.vudroid.djvudroid.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.vudroid.core.Hyperlink;
import org.vudroid.core.codec.CodecPage;

import java.util.List;

public class DjvuPage implements CodecPage {
    private long pageHandle;
    //TODO: remove all async operations
    private final Object waitObject;

    DjvuPage(long pageHandle, Object waitObject) {
        this.pageHandle = pageHandle;
        this.waitObject = waitObject;
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPage(long pageHandle, int targetWidth, int targetHeight, float pageSliceX,
                                             float pageSliceY,
                                             float pageSliceWidth,
                                             float pageSliceHeight, int[] buffer);

    private static native void free(long pageHandle);

    public int getWidth() {
        return getWidth(pageHandle);
    }

    public int getHeight() {
        return getHeight(pageHandle);
    }

    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds) {
        final int[] buffer = new int[width * height];
        renderPage(pageHandle, width, height, pageSliceBounds.left, pageSliceBounds.top, pageSliceBounds.width(), pageSliceBounds.height(), buffer);
        return Bitmap.createBitmap(buffer, width, height, Bitmap.Config.RGB_565);
    }

    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds, float scale) {
        return renderBitmap(width, height, pageSliceBounds);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (pageHandle == 0) {
            return;
        }
        free(pageHandle);
        pageHandle = 0;
    }

    @Override
    public boolean isRecycle() {
        return pageHandle == -1;
    }

    @Override
    public List<Hyperlink> getPageLinks() {
        return null;
    }

    @Override
    public void loadPage(int pageNumber) {

    }
}
