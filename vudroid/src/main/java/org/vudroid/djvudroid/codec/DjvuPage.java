package org.vudroid.djvudroid.codec;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.CodecPageInfo;
import org.vudroid.core.codec.PageLink;
import org.vudroid.core.codec.PageTextBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.link.Hyperlink;
import cn.archko.pdf.core.utils.LengthUtils;

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

    static void normalizeTextBox(final PageTextBox r, final float width, final float height) {
        final float left = r.left / width;
        final float right = r.right / width;
        final float top = 1 - r.top / height;
        final float bottom = 1 - r.bottom / height;
        r.left = Math.min(left, right);
        r.right = Math.max(left, right);
        r.top = Math.min(top, bottom);
        r.bottom = Math.max(top, bottom);
    }

    native static List<PageTextBox> getPageText(long docHandle, int pageNo, long contextHandle, String pattern);

    public static List<PageTextBox> getPageTextSync(long docHandle, int pageNo, long contextHandle, String pattern) {
        //TempHolder.lock.lock();
        try {
            return getPageText(docHandle, pageNo, contextHandle, pattern);
        } finally {
            //TempHolder.lock.unlock();
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

    public static boolean isListEmpty(List<?> objects) {
        return objects == null || objects.size() <= 0;
    }

    public String getPageHTML() {
        List<PageTextBox> pageText1 = getPageText1();
        if (isListEmpty(pageText1)) {
            return "";
        }
        StringBuilder res = new StringBuilder();
        for (PageTextBox p : pageText1) {
            res.append(p.text);
            res.append(" ");
        }

        return res.toString();
    }

    public String getPageHTMLWithImages() {
        return "";
    }

    public List<PageTextBox> getPageText1() {
        final List<PageTextBox> list = getPageTextSync(documentHandle, pageNumber, contextHandle, null);
        if (LengthUtils.isNotEmpty(list)) {
            final float width = getWidth();
            final float height = getHeight();
            for (final PageTextBox ptb : list) {
                normalizeTextBox(ptb, width, height);
            }
        }
        return list;
    }

    public Bitmap renderBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale) {
        //float patchX = (cropBound.left * scale + (int) (pageSliceBounds.left * width));
        //float patchY = (cropBound.top * scale + (int) (pageSliceBounds.top * height));
        //Log.d("TAG", String.format("page:%s, scale:%s, patchX:%s, patchY:%s, width:%s, height:%s, %s, %s", pageNumber, scale, patchX, patchY, width, height, cropBound, pageSliceBounds));

        final int[] buffer = new int[width * height];
        ///renderPage(pageHandle, contextHandle, pageW, pageH, patchX, patchY, width, height, buffer);
        renderPage(pageHandle, contextHandle, width, height, pageSliceBounds.left, pageSliceBounds.top, pageSliceBounds.width(), pageSliceBounds.height(), buffer);

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

    public List<ReflowBean> getReflowBean() {
        String rest = getPageHTML();
        List<ReflowBean> list = new ArrayList<>();
        ReflowBean bean = new ReflowBean(rest, ReflowBean.TYPE_STRING, pageHandle + "-0");
        list.add(bean);
        return list;
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

    //argb8888
    private static native boolean renderPageDirect(long pageHandle, long contextHandle,
                                                   int pageWidth, int pageHeight,
                                                   int patchX, int patchY,
                                                   int patchW, int patchH,
                                                   int[] buffer);

    private static native void free(long pageHandle);

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNo);
}
