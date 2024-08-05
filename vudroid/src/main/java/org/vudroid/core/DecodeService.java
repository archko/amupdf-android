package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.View;

import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

import cn.archko.pdf.core.entity.APage;

public interface DecodeService {

    void setContainerView(View containerView);

    CodecDocument open(String path, boolean crop, boolean cachePage);

    void decodePage(String decodeKey, PageTreeNode node, boolean crop, int pageNumber, DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds);

    void stopDecoding(String decodeKey);

    void setOriention(int oriention);

    int getEffectivePagesWidth(int page, boolean crop);

    int getEffectivePagesHeight(int index, boolean crop);

    int getPageCount();

    int getPageWidth(int pageIndex, boolean crop);

    int getPageHeight(int pageIndex, boolean crop);

    @Deprecated
    CodecPage getPage(int pageIndex);

    APage getAPage(int pageIndex);

    Outline[] getOutlines();

    void recycle();

    Bitmap decodeThumb(int page);

    interface DecodeCallback {
        void decodeComplete(Bitmap bitmap, boolean isThumb);

        boolean shouldRender(int pageNumber, boolean isFullPage);
    }
}
