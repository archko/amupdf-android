package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.View;

import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecPage;

public interface DecodeService {

    void setContainerView(View containerView);

    void open(String fileUri);

    void decodePage(String decodeKey, PageTreeNode node, int pageNumber, DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds);

    void stopDecoding(String decodeKey);

    void setOriention(int oriention);

    int getEffectivePagesWidth(int page);

    int getEffectivePagesHeight(int index);

    int getPageCount();

    int getPageWidth(int pageIndex);

    int getPageHeight(int pageIndex);

    CodecPage getPage(int pageIndex);

    Outline[] getOutlines();

    void recycle();

    public interface DecodeCallback {
        void decodeComplete(Bitmap bitmap, boolean isThumb);

        boolean shouldRender(int pageNumber, boolean isFullPage);
    }
}
