package cn.archko.pdf.core.imagedroid.codec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import org.vudroid.core.codec.CodecPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.link.Hyperlink;

public class AlbumPage implements CodecPage {

    private long pageHandle = -1;
    int pageWidth;
    int pageHeight;
    private BitmapRegionDecoder decoder;
    private String path;

    static AlbumPage createPage(String fname, int pageno) {
        AlbumPage pdfPage = new AlbumPage(pageno, fname);
        return pdfPage;
    }

    public AlbumPage(long pageno, String fname) {
        this.pageHandle = pageno;
        this.path = fname;
    }

    public int getWidth() {
        if (pageWidth == 0) {
            decodeBound();
        }
        return pageWidth;
    }

    private void decodeBound() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        pageWidth = options.outWidth;
        pageHeight = options.outHeight;
    }

    public int getHeight() {
        if (pageHeight == 0) {
            decodeBound();
        }
        return pageHeight;
    }

    public void loadPage(int pageno) {
        if (null == decoder || decoder.isRecycled()) {
            try {
                decoder = BitmapRegionDecoder.newInstance(path, false);
            } catch (IOException e) {
                Log.d("TAG", "loadPage.error,", e);
            }
        }
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
        if (null == decoder || decoder.isRecycled()) {
            loadPage(0);
        }
        if (pageHeight == 0 || pageWidth == 0) {
            decodeBound();
        }

        //缩略图
        if (pageSliceBounds.width() == 1.0f) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            final int heightRatio = Math.round((float) pageWidth / (float) height);
            final int widthRatio = Math.round((float) pageHeight / (float) width);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            options.inSampleSize = Math.max(heightRatio, widthRatio);
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            Log.d("TAG", String.format("page:%s, h-w.ratio:%s-%s, w-h:%s-%s, sample:%s",
                    pageHandle, heightRatio, widthRatio, width, height, options.inSampleSize));
            return bitmap;
        } else {
            int pageW = Math.round(width / scale);
            int pageH = Math.round(height / scale);

            int patchX = Math.round(pageSliceBounds.left * pageWidth);
            int patchY = Math.round(pageSliceBounds.top * pageHeight);
            BitmapFactory.Options options = new BitmapFactory.Options();
            final int heightRatio = Math.round((float) pageW / 1080);
            final int widthRatio = Math.round((float) pageH / 1080);
            options.inSampleSize = Math.max(heightRatio, widthRatio);

            Rect rect = new Rect(patchX, patchY, patchX + pageW, patchY + pageH);
            Bitmap bitmap = decoder.decodeRegion(rect, options);
            Log.d("TAG", String.format("page:%s, h-w.ratio:%s-%s, w-h:%s-%s, patch:%s-%s, %s, w-h:%s-%s",
                    pageHandle, heightRatio, widthRatio, pageW, pageH, patchX, patchY, pageSliceBounds, bitmap.getWidth(), bitmap.getHeight()));
            return bitmap;
        }
    }

    public List<Hyperlink> getPageLinks() {
        List<Hyperlink> hyperlinks = new ArrayList<>();

        return hyperlinks;
    }

    public synchronized void recycle() {
        if (pageHandle >= 0) {
            pageHandle = -1;
        }
        if (null != decoder) {
            decoder.recycle();
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

}
