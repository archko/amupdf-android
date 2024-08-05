//package cn.archko.pdf.common;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.widget.ImageView;
//
//import com.artifex.mupdf.fitz.Matrix;
//import com.artifex.mupdf.fitz.Page;
//import com.artifex.mupdf.fitz.RectI;
//
//import androidx.collection.LruCache;
//import cn.archko.pdf.core.App;
//import cn.archko.pdf.core.cache.BitmapCache;
//import cn.archko.pdf.core.cache.BitmapPool;
//import cn.archko.pdf.core.common.Logcat;
//import cn.archko.pdf.core.decode.MupdfDocument;
//import cn.archko.pdf.core.entity.APage;
//import cn.archko.pdf.core.decode.DecodeParam;
//
///**
// * @author: archko 2019/8/30 :16:17
// */
//public class ImageDecoder extends ImageWorker {
//
//    public static final String TAG = "ImageDecoder";
//
//    public static ImageDecoder getInstance() {
//        return Factory.instance;
//    }
//
//    private static final class Factory {
//
//        private static final ImageDecoder instance = new ImageDecoder(App.Companion.getInstance());
//    }
//
//    private ImageDecoder(final Context context) {
//        super(context);
//    }
//
//    @Override
//    public boolean isScrolling() {
//        //if (mImageCache != null) {
//        //    //return mImageCache.isScrolling();
//        //}
//        return false;
//    }
//
//    @Override
//    public void addBitmapToCache(final String key, final Bitmap bitmap) {
//        BitmapCache.getInstance().addBitmap(key, bitmap);
//    }
//
//    @Override
//    public Bitmap getBitmapFromCache(final String key) {
//        return BitmapCache.getInstance().getBitmap(key);
//    }
//
//    @Override
//    public LruCache<String, APage> getPageLruCache() {
//        return null;
//    }
//
//    public static String getCacheKey(int index, boolean crop, float zoom) {
//        return String.format("%s-%s-%s", index, crop, zoom);
//    }
//
//    @Override
//    protected Bitmap processBitmap(DecodeParam decodeParam) {
//        try {
//            //long start = SystemClock.uptimeMillis();
//            Page page = decodeParam.getDocument().loadPage(decodeParam.getPageNum());
//            //if (decodeParam.pageNum != decodeParam.pageSize.index) {
//            if (!decodeParam.getDecodeCallback().shouldRender(decodeParam.getPageNum(), decodeParam)) {
//                if (Logcat.loggable) {
//                    Logcat.w(TAG, String.format("decode cancel1,index changed: %s-%s,page:%s",
//                            decodeParam.getPageNum(), decodeParam.getPageSize().index, decodeParam.getPageSize()));
//                }
//                return null;
//            }
//
//            int leftBound = 0;
//            int topBound = 0;
//            final APage pageSize = decodeParam.getPageSize();
//            int pageW = pageSize.getZoomPoint().x;
//            int pageH = pageSize.getZoomPoint().y;
//
//            Matrix ctm = new Matrix(2);
//            RectI bbox = new RectI(page.getBounds().transform(ctm));
//            float xscale = (float) pageW / (float) (bbox.x1 - bbox.x0);
//            float yscale = (float) pageH / (float) (bbox.y1 - bbox.y0);
//            ctm.scale(xscale, yscale);
//
//            if ((pageSize.getTargetWidth() > 0)) {
//                pageW = pageSize.getTargetWidth();
//            }
//
//            if (decodeParam.getCrop()) {
//                if (pageSize.getCropWidth() != pageW && pageSize.getCropHeight() != pageH) {
//                } else {
//                    float[] arr = MupdfDocument.Companion.getArrByCrop(page, ctm, pageW, pageH, leftBound, topBound);
//                    leftBound = (int) arr[0];
//                    topBound = (int) arr[1];
//                    pageH = (int) arr[2];
//                    float cropScale = arr[3];
//
//                    pageSize.setCropHeight(pageH);
//                    pageSize.setCropWidth(pageW);
//                    pageSize.setCropScale(cropScale);
//                    //RectF cropRectf = new RectF(leftBound, topBound, leftBound + pageW, topBound + pageH);
//                    //pageSize.setCropBounds(cropRectf, cropScale);
//                }
//            }
//
//            //if (decodeParam.pageNum != decodeParam.pageSize.index) {
//            if (!decodeParam.getDecodeCallback().shouldRender(decodeParam.getPageNum(), decodeParam)) {
//                if (Logcat.loggable) {
//                    Logcat.w(TAG, String.format("decode cancel2,index changed: %s-%s,page:%s",
//                            decodeParam.getPageNum(), decodeParam.getPageSize().index, decodeParam.getPageSize()));
//                }
//                return null;
//            }
//
//            Bitmap bitmap = BitmapCache.getInstance().getBitmap(decodeParam.getKey());
//            if (null != bitmap) {
//                return bitmap;
//            }
//            if (Logcat.loggable) {
//                Logcat.d(TAG, String.format("decode bitmap:%s-%s, %s-%s,page:%s-%s, bound(left-top):%s-%s, page:%s",
//                        decodeParam.getPageNum(), decodeParam.getPageSize().index, pageW, pageH, pageSize.getZoomPoint().x, pageSize.getZoomPoint().y,
//                        leftBound, topBound, pageSize));
//            }
//
//            bitmap = BitmapPool.getInstance().acquire(pageW, pageH);//Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
//            MupdfDocument.Companion.render(page, ctm, bitmap, decodeParam.getXOrigin(), leftBound, topBound);
//            //if (decodeParam.pageNum != decodeParam.pageSize.index) {
//            if (!decodeParam.getDecodeCallback().shouldRender(decodeParam.getPageNum(), decodeParam)) {
//                if (Logcat.loggable) {
//                    Logcat.w(TAG, String.format("decode cancel3,index changed: %s-%s,page:%s",
//                            decodeParam.getPageNum(), decodeParam.getPageSize().index, decodeParam.getPageSize()));
//                }
//                addBitmapToCache(decodeParam.getKey(), bitmap);
//                return null;
//            }
//
//            page.destroy();
//            //Logcat.d(TAG, "decode:" + (SystemClock.uptimeMillis() - start));
//            return bitmap;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    protected void postBitmap(BitmapWorkerTask bitmapWorkerTask, Bitmap bitmap, DecodeParam decodeParam) {
//        if (bitmapWorkerTask.isCancelled() || bitmap == null) {
//            Logcat.w(TAG, "cancel decode.");
//            return;
//        }
//        final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
//        if (imageView != null) {
//            if (null != decodeParam.getDecodeCallback()) {
//                decodeParam.getDecodeCallback().decodeComplete(bitmap, decodeParam);
//            }
//        }
//    }
//
//    @Override
//    public void recycle() {
//        BitmapCache.getInstance().clear();
//    }
//}
