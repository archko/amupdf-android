package org.vudroid.djvudroid.codec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.CodecPageInfo;
import org.vudroid.core.codec.PageLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.link.Hyperlink;

public class DjvuPage implements CodecPage {
    private long pageHandle;
    private long contextHandle;
    private long documentHandle;
    private int width = 1;
    private int height = 1;
    private int pageNumber;

    DjvuPage(long contextHandle, long documentHandle, long pageHandle, int pageNumber) {
        this.pageHandle = pageHandle;
        this.contextHandle = contextHandle;
        this.documentHandle = documentHandle;
        this.pageNumber = pageNumber;
        getPageInfo(contextHandle, documentHandle, pageNumber);
    }

    public void getPageInfo(long contextHandle, long documentHandle, final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final int res = getPageInfo(documentHandle, pageNumber, contextHandle, info);
        if (res > -1) {
            width = info.width;
            height = info.height;
        }
    }

    public int getWidth() {
        if (width > 1) {
            return width;
        }
        return getWidth(pageHandle);
    }

    public int getHeight() {
        if (height > 1) {
            return height;
        }
        return getHeight(pageHandle);
    }

    public Bitmap renderBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale) {
        int pageW;
        int pageH;
        int patchX;
        int patchY;
        //如果页面的缩放为1,那么这时的pageW就是view的宽.这里先将切边后比例把原始图缩放一次
        pageW = (int) (this.width * scale);
        pageH = (int) (this.height * scale);

        //scale = viewwidth / pagewidth,前者是显示宽,后者是缩放切边后的宽
        //width = scale * pagewidth * bound.width = viewwidth * bound.width
        patchX = (int) ((int) (pageW * pageSliceBounds.left) + cropBound.left * scale);
        patchY = (int) ((int) (pageH * pageSliceBounds.top) + cropBound.top * scale);
        Log.d("TAG", String.format("scale:%s, patchX:%s, patchY:%s,pageW:%s, pageH:%s, width:%s, height:%s, %s, %s", scale, patchX, patchY, pageW, pageH, width, height, cropBound, pageSliceBounds));

        final int[] buffer = new int[width * height];
        renderPage2(pageHandle, contextHandle, pageW, pageH, patchX, patchY, width, height, buffer);

        Bitmap bitmap = BitmapPool.getInstance().acquire(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(buffer, 0, width, 0, 0, width, height);
        return bitmap;
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
        final List<Hyperlink> hyperlinks = new ArrayList<>();
        final List<PageLink> links = getPageLinks(documentHandle, pageNumber);
        if (links != null) {
            final float width = getWidth();
            final float height = getHeight();
            for (final PageLink link : links) {
                normalize(link.sourceRect, width, height);

                Hyperlink hyperlink = new Hyperlink();
                if (link.url != null && link.url.startsWith("#")) {
                    try {
                        link.targetPage = Integer.parseInt(link.url.substring(1)) - 1;
                        link.url = null;
                        hyperlink.setLinkType(Hyperlink.LINKTYPE_URL);
                    } catch (final NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    hyperlink.setLinkType(Hyperlink.LINKTYPE_PAGE);
                }
                hyperlink.setPage(link.targetPage);
                hyperlink.setUrl(link.url);
                hyperlink.setBbox(new Rect((int) link.sourceRect.left,
                        (int) link.sourceRect.top,
                        (int) link.sourceRect.top,
                        (int) link.sourceRect.bottom));
                hyperlinks.add(hyperlink);
            }

            return hyperlinks;
        }
        return Collections.emptyList();
    }

    static void normalize(final RectF r, final float width, final float height) {
        r.left = r.left / width;
        r.right = r.right / width;
        r.top = r.top / height;
        r.bottom = r.bottom / height;
    }

    @Override
    public void loadPage(int pageNumber) {

    }

    private native static int getPageInfo(long docHandle, int pageNumber, long contextHandle, CodecPageInfo cpi);

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean renderPage(long pageHandle, long contextHandle,
                                             int targetWidth, int targetHeight,
                                             float pageSliceX, float pageSliceY,
                                             float pageSliceWidth, float pageSliceHeight,
                                             int[] buffer);

    private static native boolean renderPage2(long pageHandle, long contextHandle,
                                              int pageWidth, int pageHeight,
                                              int patchX, int patchY,
                                              int patchW, int patchH,
                                              int[] buffer);

    //argb8888
    private static native boolean renderPageDirect(long pageHandle, long contextHandle,
                                                   int pageWidth, int pageHeight,
                                                   int patchX, int patchY,
                                                   int patchW, int patchH,
                                                   int[] buffer);

    private static native void free(long pageHandle);

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNo);
}
