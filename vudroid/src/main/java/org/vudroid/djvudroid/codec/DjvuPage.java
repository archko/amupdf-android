package org.vudroid.djvudroid.codec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.archko.reader.image.DjvuLink;
import com.archko.reader.image.DjvuLoader;
import com.archko.reader.image.DjvuPageInfo;
import com.archko.reader.image.TextSearchResult;

import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.PageTextBox;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.link.Hyperlink;

public class DjvuPage implements CodecPage {
    private final DjvuLoader djvuLoader;
    private int width = -1;
    private int height = -1;
    private final int pageNumber;

    DjvuPage(DjvuLoader djvuLoader, int pageNumber) {
        this.djvuLoader = djvuLoader;
        this.pageNumber = pageNumber;
    }

    public int getWidth() {
        if (width == -1) {
            DjvuPageInfo pageInfo = djvuLoader.getPageInfo(pageNumber);
            if (pageInfo != null) {
                width = pageInfo.getWidth();
            } else {
                width = 0;
            }
        }
        return width;
    }

    public int getHeight() {
        if (height == -1) {
            DjvuPageInfo pageInfo = djvuLoader.getPageInfo(pageNumber);
            if (pageInfo != null) {
                height = pageInfo.getHeight();
            } else {
                height = 0;
            }
        }
        return height;
    }

    public static List<PageTextBox> getPageTextSync(DjvuLoader loader, int pageNo, String pattern) {
        List<PageTextBox> list = new ArrayList<>();
        if (pattern != null && !pattern.isEmpty()) {
            List<TextSearchResult> searchResults = loader.searchText(pageNo, pattern);
            if (searchResults != null) {
                for (TextSearchResult result : searchResults) {
                    System.out.println(String.format("TextSearchResult:%s", result));
                    PageTextBox ptb = new PageTextBox();
                    ptb.text = result.getText();
                    ptb.left = result.getX();
                    ptb.top = result.getY();
                    ptb.right = result.getWidth();
                    ptb.bottom = result.getHeight();
                    list.add(ptb);
                }
            }
        }
        return list;
    }

    /**
     * 解码方法decodeRegionToBitmap的输出高宽是
     * uint32_t out_width = (uint32_t) (width * scale);
     * uint32_t out_height = (uint32_t) (height * scale);
     * 所以传入的要*scale.
     * 偏移量:
     * uint32_t scaled_x = (uint32_t)(x * scale);
     * uint32_t scaled_y = (uint32_t)(y * scale);
     *
     * @param cropBound       这是原始高宽的rect,如果有切边,也是按原始高宽切的.
     * @param targetWidth     这是经过所有的缩放后目标宽
     * @param targetHeight    这是经过所有的缩放后目标高
     * @param pageSliceBounds
     * @param scale           这是view的宽/原始页面*view缩放zoom
     * @return
     */
    public Bitmap renderBitmap(Rect cropBound, int targetWidth, int targetHeight, RectF pageSliceBounds, float scale) {
        // 1. 基于目标尺寸反推原始区域尺寸
        int originalWidth = Math.round(targetWidth / scale);
        int originalHeight = Math.round(targetHeight / scale);

        // 2. 计算原始坐标系中的起始位置
        int originalX = (int) (cropBound.left + pageSliceBounds.left * cropBound.width());
        int originalY = (int) (cropBound.top + pageSliceBounds.top * cropBound.height());
        Log.d("TAG", String.format("renderPageRegion:%s, scale:%s, patchX:%s, patchY:%s, target.w-h:%s-%s, page.w-h:%s-%s, %s, %s",
                pageNumber, scale, originalX, originalY, targetWidth, targetHeight, originalWidth, originalHeight, cropBound, pageSliceBounds));
        //renderPageRegion:0, scale:0.07058824, patchX:0, patchY:0, target.w-h:180-232, page.w-h:2550-3287, Rect(0, 0 - 2550, 3300), RectF(0.0, 0.0, 1.0, 1.0)
        //renderPageRegion:0, scale:0.28235295, patchX:0, patchY:1650, target.w-h:360-466, page.w-h:1275-1650, Rect(0, 0 - 2550, 3300), RectF(0.0, 0.5, 0.5, 1.0)

        // 4. 调用JNI方法
        Bitmap bitmap = djvuLoader.decodeRegionToBitmap(
                pageNumber,
                originalX,
                originalY,
                originalWidth,
                originalHeight,
                scale
        );

        return bitmap != null ? bitmap : BitmapPool.getInstance().acquire(targetWidth, targetHeight, Bitmap.Config.RGB_565);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        // No JNI to free
    }

    @Override
    public boolean isRecycle() {
        return false;
    }

    @Override
    public List<Hyperlink> getPageLinks() {
        List<DjvuLink> links = djvuLoader.getPageLinks(pageNumber);

        if (links == null) {
            return new ArrayList<>();
        }

        List<Hyperlink> hyperlinks = new ArrayList<>();

        for (DjvuLink link : links) {
            Hyperlink hyperlink = new Hyperlink();
            hyperlink.setBbox(new Rect(
                    link.getX(),
                    link.getY(),
                    (link.getX() + link.getWidth()),
                    (link.getY() + link.getHeight())
            ));

            if (link.getPage() >= 0) {
                // 页面链接
                hyperlink.setLinkType(Hyperlink.LINKTYPE_PAGE);
                hyperlink.setPage(link.getPage());
                hyperlink.setUrl(null);
            } else if (link.getUrl() != null) {
                // URL链接
                hyperlink.setLinkType(Hyperlink.LINKTYPE_URL);
                hyperlink.setUrl(link.getUrl());
                hyperlink.setPage(-1);
            } else {
                // 无效链接，跳过
                continue;
            }

            hyperlinks.add(hyperlink);
        }
        return hyperlinks;
    }

    public List<ReflowBean> getReflowBean() {
        String text = djvuLoader.getPageText(pageNumber);
        List<ReflowBean> list = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            ReflowBean bean = new ReflowBean(text, ReflowBean.TYPE_STRING, pageNumber + "-0");
            list.add(bean);
        }
        return list;
    }

    @Override
    public void loadPage(int pageNumber) {
        // No-op
    }
}
