package org.vudroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.vudroid.core.Hyperlink;

import java.util.List;

public interface CodecPage {

    int getWidth();

    int getHeight();

    //Bitmap renderBitmap(int width, int height, RectF pageSliceBounds);
    Bitmap renderBitmap(int width, int height, RectF pageSliceBounds, float scale);

    void recycle();

    boolean isRecycle();

    List<Hyperlink> getPageLinks();

    void loadPage(int pageNumber);
}
