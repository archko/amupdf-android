package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.RectI;

import androidx.collection.LruCache;
import cn.archko.pdf.App;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.listeners.DecodeCallback;
import cn.archko.pdf.mupdf.MupdfDocument;

/**
 * @author: archko 2019/8/30 :16:17
 */
public class ImageDecoder extends ImageWorker {

    public static final String TAG = "ImageDecoder";
    private LruCache<Object, Bitmap> mImageCache = BitmapCache.getInstance().getCache();

    public static ImageDecoder getInstance() {
        return Factory.instance;
    }

    private static final class Factory {

        private static final ImageDecoder instance = new ImageDecoder(App.Companion.getInstance());
    }

    private ImageDecoder(final Context context) {
        super(context);
        mContext = context.getApplicationContext();
        mResources = mContext.getResources();
    }

    @Override
    public boolean isScrolling() {
        if (mImageCache != null) {
            //return mImageCache.isScrolling();
        }
        return false;
    }

    @Override
    public void addBitmapToCache(final String key, final Bitmap bitmap) {
        if (mImageCache != null) {
            mImageCache.put(key, bitmap);
        }
    }

    @Override
    public Bitmap getBitmapFromCache(final String key) {
        if (mImageCache != null) {
            return mImageCache.get(key);
        }
        return null;
    }

    @Override
    public LruCache<Object, Bitmap> getImageCache() {
        return mImageCache;
    }

    @Override
    public LruCache<String, APage> getPageLruCache() {
        return null;
    }

    public void loadImage(APage aPage, boolean crop, int xOrigin,
                          ImageView imageView, Document document, DecodeCallback callback) {
        if (document == null || aPage == null || imageView == null) {
            return;
        }
        super.loadImage(new DecodeParam(getCacheKey(aPage.index, crop, aPage.getScaleZoom()),
                imageView, crop, xOrigin, aPage, document, callback));
    }

    public static String getCacheKey(int index, boolean crop, float zoom) {
        return String.format("%s-%s-%s", index, crop, zoom);
    }

    @Override
    protected Bitmap processBitmap(DecodeParam decodeParam) {
        try {
            //long start = SystemClock.uptimeMillis();
            Page page = decodeParam.document.loadPage(decodeParam.pageSize.index);

            int leftBound = 0;
            int topBound = 0;
            APage pageSize = decodeParam.pageSize;
            int pageW = pageSize.getZoomPoint().x;
            int pageH = pageSize.getZoomPoint().y;

            Matrix ctm = new Matrix(MupdfDocument.ZOOM);
            RectI bbox = new RectI(page.getBounds().transform(ctm));
            float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
            float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
            ctm.scale(xscale, yscale);

            if ((pageSize.getTargetWidth() > 0)) {
                pageW = pageSize.getTargetWidth();
            }

            if (decodeParam.crop) {
                float[] arr = MupdfDocument.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound);
                leftBound = (int) arr[0];
                topBound = (int) arr[1];
                pageH = (int) arr[2];
                float cropScale = arr[3];

                pageSize.setCropHeight(pageH);
                pageSize.setCropWidth(pageW);
                //RectF cropRectf = new RectF(leftBound, topBound, leftBound + pageW, topBound + pageH);
                //pageSize.setCropBounds(cropRectf, cropScale);
            }
            if (Logcat.loggable) {
                Logcat.d(TAG, String.format("decode bitmap: %s-%s,page:%s-%s,xOrigin:%s, bound(left-top):%s-%s, page:%s",
                        pageW, pageH, pageSize.getZoomPoint().x, pageSize.getZoomPoint().y,
                        decodeParam.xOrigin, leftBound, topBound, pageSize));
            }

            Bitmap bitmap = BitmapPool.getInstance().acquire(pageW, pageH);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

            MupdfDocument.render(page, ctm, bitmap, decodeParam.xOrigin, leftBound, topBound);

            page.destroy();
            //Logcat.d(TAG, "decode:" + (SystemClock.uptimeMillis() - start));
            BitmapCache.getInstance().addBitmap(decodeParam.key, bitmap);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void postBitmap(BitmapWorkerTask bitmapWorkerTask, Bitmap bitmap, DecodeParam decodeParam) {
        if (bitmapWorkerTask.isCancelled() || bitmap == null) {
            Logcat.w(TAG, "cancel decode.");
            return;
        }
        final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
        if (imageView != null) {
            addBitmapToCache(getCacheKey(decodeParam.pageSize.index, decodeParam.crop, decodeParam.pageSize.getScaleZoom()), bitmap);

            if (null != decodeParam.decodeCallback) {
                decodeParam.decodeCallback.decodeComplete(bitmap);
            }
        }
    }

    @Override
    public void recycle() {
        mImageCache.evictAll();
    }
}
