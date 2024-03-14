package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.view.View;

import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecPage;

public interface DecodeService {

    void setContainerView(View containerView);

    void open(String fileUri);

    void decodePage(Object decodeKey, PageTreeNode node, DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds);

    void stopDecoding(Object decodeKey);

    void setOriention(int oriention);

    int getEffectivePagesWidth();

    int getEffectivePagesHeight();

    int getPageCount();

    int getPageWidth(int pageIndex);

    int getPageHeight(int pageIndex);

    CodecPage getPage(int pageIndex);

    Outline[] getOutlines();

    void recycle();

    public interface DecodeCallback {
        void decodeComplete(Bitmap bitmap);
    }
}
