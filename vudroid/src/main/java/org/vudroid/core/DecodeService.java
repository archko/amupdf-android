package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.View;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.OutlineLink;
import org.vudroid.core.codec.PageTextBox;

import java.util.List;

import cn.archko.pdf.core.common.APageSizeLoader;
import cn.archko.pdf.core.entity.APage;

public interface DecodeService {

    void setContainerView(View containerView);

    CodecDocument open(String path, boolean cachePage, boolean crop);

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

    void resetCrop();

    /**
     * 从页面坐标获取选中的文本
     * @param pageIndex 页面索引
     * @param startX 起始点X坐标（页面坐标）
     * @param startY 起始点Y坐标（页面坐标）
     * @param endX 结束点X坐标（页面坐标）
     * @param endY 结束点Y坐标（页面坐标）
     * @return 选中的文本
     */
    String getSelectedText(int pageIndex, float startX, float startY, float endX, float endY);

    /**
     * 获取文本选择的高亮区域
     * @param pageIndex 页面索引
     * @param startX 起始点X坐标（页面坐标）
     * @param startY 起始点Y坐标（页面坐标）
     * @param endX 结束点X坐标（页面坐标）
     * @param endY 结束点Y坐标（页面坐标）
     * @return 高亮区域列表
     */
    List<RectF> getTextSelectionRects(int pageIndex, float startX, float startY, float endX, float endY);

    interface DecodeCallback {
        void decodeComplete(Bitmap bitmap, boolean isThumb, Object args);

        boolean shouldRender(int pageNumber, boolean isFullPage);
    }

    interface SearchCallback {
        void result(List<PageTextBox> result, int index);
    }
}
