package org.vudroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

import cn.archko.pdf.core.link.Hyperlink;

public interface CodecPage {

    int getWidth();

    int getHeight();

    //Bitmap renderBitmap(int width, int height, RectF pageSliceBounds);
    Bitmap renderBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale);

    void recycle();

    boolean isRecycle();

    List<Hyperlink> getPageLinks();

    void loadPage(int pageNumber);
}
