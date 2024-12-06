package org.vudroid.pdfdroid.codec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import org.vudroid.core.codec.CodecPage;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.link.Hyperlink;


public class PdfPage implements CodecPage {

    private long pageHandle = -1;
    Page page;
    int pdfPageWidth;
    int pdfPageHeight;
    private Document doc;

    public PdfPage(Document core, long pageHandle) {
        this.pageHandle = pageHandle;
        this.doc = core;
    }

    public int getWidth() {
        //return (int) getMediaBox().width();
        if (pdfPageWidth == 0) {
            pdfPageWidth = (int) (page.getBounds().x1 - page.getBounds().x0);
        }
        return pdfPageWidth;
    }

    public int getHeight() {
        //return (int) getMediaBox().height();
        if (pdfPageHeight == 0) {
            pdfPageHeight = (int) (page.getBounds().y1 - page.getBounds().y0);
        }
        return pdfPageHeight;
    }

    public void loadPage(int pageno) {
        page = doc.loadPage(pageno);
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return page;
    }

    public static PdfPage createPage(Document core, int pageno) {
        PdfPage pdfPage = new PdfPage(core, pageno);
        pdfPage.page = core.loadPage(pageno);
        return pdfPage;
    }

    /**
     * 解码
     *
     * @param width           一个页面的宽
     * @param height          一个页面的高
     * @param pageSliceBounds 每个页面的边框
     * @param scale           缩放级别
     * @return 位图
     */
    public Bitmap renderBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale) {
        //Matrix matrix=new Matrix();
        //matrix.postScale(width/getWidth(), -height/getHeight());
        //matrix.postTranslate(0, height);
        //matrix.postTranslate(-pageSliceBounds.left*width, -pageSliceBounds.top*height);
        //matrix.postScale(1/pageSliceBounds.width(), 1/pageSliceBounds.height());

        int pageW;
        int pageH;
        int patchX;
        int patchY;
        //如果页面的缩放为1,那么这时的pageW就是view的宽.
        pageW = (int) (cropBound.width() * scale);
        pageH = (int) (cropBound.height() * scale);

        patchX = (int) ((int) (pageSliceBounds.left * pageW) + cropBound.left * scale);
        patchY = (int) ((int) (pageSliceBounds.top * pageH) + cropBound.top * scale);
        Bitmap bitmap = BitmapPool.getInstance().acquire(width, height);

        //Log.d("TAG", String.format("page:%s, scale:%s, patchX:%s, patchY:%s, width:%s, height:%s, %s", pageHandle, scale, patchX, patchY, width, height, cropBound));

        com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(scale);
        AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, patchX, patchY, 0, 0, width, height);

        if (pageHandle == -1) {
            return bitmap;
        }
        try {
            page.run(dev, ctm, null);
        } catch (Exception e) {
        }
        dev.close();
        dev.destroy();

        return bitmap;
    }

    public List<Hyperlink> getPageLinks() {
        List<Hyperlink> hyperlinks = new ArrayList<>();
        Link[] links = page.getLinks();
        if (null != links) {
            for (Link link : links) {
                Hyperlink hyper = new Hyperlink();
                Location loc = doc.resolveLink(link);
                int page = doc.pageNumberFromLocation(loc);
                hyper.setPage(page);
                hyper.setBbox(new Rect((int) link.getBounds().x0, (int) link.getBounds().y0, (int) link.getBounds().x1, (int) link.getBounds().y1));

                if (page >= 0) {
                    hyper.setLinkType(Hyperlink.LINKTYPE_PAGE);
                } else {
                    hyper.setUrl(link.getURI());
                    hyper.setLinkType(Hyperlink.LINKTYPE_URL);
                }
                hyperlinks.add(hyper);
            }
        }

        return hyperlinks;
    }

    public synchronized void recycle() {
        if (pageHandle >= 0) {
            pageHandle = -1;
            if (page != null) {
                page.destroy();
            }
        }
    }

    @Override
    public boolean isRecycle() {
        return pageHandle == -1;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public Bitmap renderBitmap(int width, int height, RectF pageSliceBounds) {
        Matrix matrix = new Matrix();
        //matrix.postScale(width/getMediaBox().width(), -height/getMediaBox().height());
        matrix.postTranslate(0, height);
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());
        return render(new Rect(0, 0, width, height), matrix);
    }

    public Bitmap render(Rect viewbox, Matrix matrix) {
        int[] mRect = new int[4];
        mRect[0] = viewbox.left;
        mRect[1] = viewbox.top;
        mRect[2] = viewbox.right;
        mRect[3] = viewbox.bottom;

        float[] matrixSource = new float[9];
        float[] matrixArray = new float[6];
        matrix.getValues(matrixSource);
        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        int width = viewbox.width();
        int height = viewbox.height();
        int[] bufferarray = new int[width * height];
        //nativeCreateView(docHandle, pageHandle, mRect, matrixArray, bufferarray);
        return Bitmap.createBitmap(bufferarray, width, height, Bitmap.Config.RGB_565);
    }

}
