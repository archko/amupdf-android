package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.View;

import com.artifex.mupdf.fitz.Quad;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.OutlineLink;

import java.util.List;

import cn.archko.pdf.core.common.APageSizeLoader;
import cn.archko.pdf.core.entity.APage;

public interface DecodeService {

    void setContainerView(View containerView);

    CodecDocument open(String path, boolean cachePage);

    APageSizeLoader.PageSizeBean getPageSizeBean();

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

    List<OutlineLink> getOutlines();

    void recycle();

    Bitmap decodeThumb(int page);

    void prev(String text, int page, SearchCallback sc);

    void next(String text, int page, SearchCallback sc);

    interface DecodeCallback {
        void decodeComplete(Bitmap bitmap, boolean isThumb, Object args);

        boolean shouldRender(int pageNumber, boolean isFullPage);
    }

    interface SearchCallback {
        void result(Object[] result, int index);
    }
}
