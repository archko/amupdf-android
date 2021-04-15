package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import androidx.collection.LruCache;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;

import java.io.File;

import cn.archko.pdf.App;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.mupdf.MupdfDocument;
import cn.archko.pdf.utils.BitmapUtils;
import cn.archko.pdf.utils.FileUtils;

/**
 * @author: archko 2019/8/30 :16:17
 */
public class ImageLoader extends ImageWorker {

    private LruCache<Object, Bitmap> mImageCache = new LruCache<>(32);
    private LruCache<String, APage> pageLruCache = new LruCache<>(64);

    public static ImageLoader getInstance() {
        return Factory.instance;
    }

    private static final class Factory {
        private static final ImageLoader instance = new ImageLoader(App.Companion.getInstance());
    }

    private ImageLoader(final Context context) {
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
        return pageLruCache;
    }

    public void loadImage(final String key, int pageNum, float zoom, int screenWidth, final ImageView imageView) {
        if (key == null || getImageCache() == null || imageView == null) {
            return;
        }
        super.loadImage(new DecodeParam(key, pageNum, zoom, screenWidth, imageView), false);
    }

    @Override
    protected Bitmap processBitmap(DecodeParam decodeParam) {
        Bitmap bitmap = null;
        try {
            File thumb = FileUtils.getDiskCacheDir(App.Companion.getInstance(), FileUtils.getRealPath(decodeParam.key));
            bitmap = decodeFromFile(thumb);
            if (null == bitmap) {
                File file = new File(decodeParam.key);
                if (file.exists()) {
                    bitmap = decodeFromPDF(decodeParam.key, decodeParam.pageNum, decodeParam.zoom, decodeParam.screenWidth);
                    if (bitmap != null) {
                        BitmapUtils.saveBitmapToFile(bitmap, thumb);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Logcat.d("decode:" + bitmap + " key:" + key);

        return bitmap;

    }

    protected Bitmap decodeFromFile(File file) {
        if (file.exists()) {
            return BitmapUtils.decodeFile(file);
        }
        return null;
    }

    protected Bitmap decodeFromPDF(String key, int pageNum, float zoom, int screenWidth) {
        Document mDocument = Document.openDocument(key);

        Page p = mDocument.loadPage(0);
        Rect b = p.getBounds();
        float w = b.x1 - b.x0;
        float h = b.y1 - b.y0;
        PointF pointf = new PointF(w, h);
        APage aPage = getPageLruCache().get(key);
        if (null == aPage) {
            aPage = new APage(pageNum, pointf, zoom, screenWidth / 3);
            getPageLruCache().put(key, aPage);
        }

        Page page = mDocument.loadPage(aPage.index);
        Point zoomSize = aPage.getZoomPoint();
        float scale = 1.0f;
        int leftBound = 0;
        int topBound = 0;
        Matrix ctm = new Matrix(aPage.getScaleZoom() * scale);
        Bitmap bm = Bitmap.createBitmap(zoomSize.x, zoomSize.y, Bitmap.Config.ARGB_8888);
        MupdfDocument.render(p, ctm, bm, 0, leftBound, topBound);
        page.destroy();
        return bm;
    }

    @Override
    protected void postBitmap(BitmapWorkerTask bitmapWorkerTask, Bitmap result, DecodeParam decodeParam) {
        if (bitmapWorkerTask.isCancelled()) {
            result = null;
        }
        final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
        if (result != null && imageView != null) {  //TODO if load image failed...
            if (null == imageView.getDrawable() || imageView.getDrawable() instanceof BitmapDrawable) {
                imageView.setImageBitmap(result);
            } else {
                Bitmap bitmap = getBitmapFromCache(decodeParam.key);
                if (null != bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public void recycle() {
        mImageCache.evictAll();
        pageLruCache.evictAll();
    }
}
