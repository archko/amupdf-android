package org.vudroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.util.List;

import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.link.Hyperlink;

public interface CodecPage {

    int getWidth();

    int getHeight();

    //Bitmap renderBitmap(int width, int height, RectF pageSliceBounds);
    Bitmap renderBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale);

    void recycle();

    boolean isRecycle();

    List<Hyperlink> getPageLinks();

    List<ReflowBean> getReflowBean();

    void loadPage(int pageNumber);

    /**
     * 获取选中的文本
     * @param startX 起始点X坐标（页面坐标）
     * @param startY 起始点Y坐标（页面坐标）
     * @param endX 结束点X坐标（页面坐标）
     * @param endY 结束点Y坐标（页面坐标）
     * @return 选中的文本
     */
    String getSelectedText(float startX, float startY, float endX, float endY);

    /**
     * 获取文本选择的高亮区域
     * @param startX 起始点X坐标（页面坐标）
     * @param startY 起始点Y坐标（页面坐标）
     * @param endX 结束点X坐标（页面坐标）
     * @param endY 结束点Y坐标（页面坐标）
     * @return 高亮区域列表
     */
    List<RectF> getTextSelectionRects(float startX, float startY, float endX, float endY);
}
