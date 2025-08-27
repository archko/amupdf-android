package cn.archko.pdf.imagedroid.codec;

import android.graphics.*;
import android.util.Log;

import com.archko.reader.image.TiffInfo;
import com.archko.reader.image.TiffLoader;

import org.vudroid.core.codec.CodecPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.link.Hyperlink;

public class AlbumPage implements CodecPage {

    public static final int MAX_WIDTH = 768;
    private long pageHandle = -1;
    int pageWidth;
    int pageHeight;
    private BitmapRegionDecoder decoder;
    private String path;
    private TiffLoader tiffLoader;
    private boolean isTiff = false;

    static AlbumPage createPage(String fname, int pageno) {
        AlbumPage pdfPage = new AlbumPage(pageno, fname);
        return pdfPage;
    }

    public AlbumPage(long pageno, String fname) {
        this.pageHandle = pageno;
        this.path = fname;
        isTiff = IntentFile.INSTANCE.isTiffImage(fname);
    }

    public int getWidth() {
        if (pageWidth == 0) {
            decodeBound();
        }
        return pageWidth;
    }

    private void decodeBound() {
        if (isTiff) {
            if (tiffLoader == null) {
                tiffLoader = new TiffLoader();
            }
            tiffLoader.openTiff(path);
            TiffInfo tiffInfo = tiffLoader.getTiffInfo();
            pageWidth = tiffInfo.getWidth();
            pageHeight = tiffInfo.getHeight();
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            pageWidth = options.outWidth;
            pageHeight = options.outHeight;
        }
    }

    public int getHeight() {
        if (pageHeight == 0) {
            decodeBound();
        }
        return pageHeight;
    }

    public void loadPage(int pageno) {
        if (isTiff) {
            if (tiffLoader == null) {
                tiffLoader = new TiffLoader();
            }
        } else if (null == decoder || decoder.isRecycled()) {
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
        if (!isTiff) {
            return renderImageBitmap(cropBound, width, height, pageSliceBounds, scale);
        } else {
            return renderTifBitmap(cropBound, width, height, pageSliceBounds, scale);
        }
    }

    public Bitmap renderImageBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale) {
        loadPage(0);

        if (pageHeight == 0 || pageWidth == 0) {
            decodeBound();
        }

        //缩略图
        if (pageSliceBounds.width() == 1.0f) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int widthRatio = 2;
            int tWidth = pageWidth;
            while (tWidth > MAX_WIDTH) {
                tWidth = pageWidth / widthRatio;
                widthRatio *= 2;
            }
            int heightRatio = 2;
            int tHeight = pageWidth;
            while (tHeight > MAX_WIDTH) {
                tHeight = pageHeight / heightRatio;
                heightRatio *= 2;
            }
            //final int heightRatio = Math.round((float) pageHeight / (float) height);
            //final int widthRatio = Math.round((float) pageWidth / (float) width);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            options.inSampleSize = Math.max(heightRatio, widthRatio);
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            Log.d("TAG", String.format("page:%s, %s-%s, h-w.ratio:%s-%s, w-h:%s-%s, sample:%s",
                    pageHandle, pageWidth, pageHeight, heightRatio, widthRatio, width, height, options.inSampleSize));
            return bitmap;
        } else {
            int pageW = Math.round(width / scale);
            int pageH = Math.round(height / scale);

            int patchX = Math.round(pageSliceBounds.left * pageWidth);
            int patchY = Math.round(pageSliceBounds.top * pageHeight);

            // 确保区域不超出边界
            patchX = Math.max(0, Math.min(patchX, pageWidth));
            patchY = Math.max(0, Math.min(patchY, pageHeight));

            int endX = Math.min(patchX + pageW, pageWidth);
            int endY = Math.min(patchY + pageH, pageHeight);

            BitmapFactory.Options options = new BitmapFactory.Options();
            Rect rect = new Rect(patchX, patchY, endX, endY);
            
            // 检查rect的宽高是否小于1
            if (rect.width() < 1 || rect.height() < 1) {
                // 创建1024x800的错误位图，中间绘制error文字
                Bitmap errorBitmap = Bitmap.createBitmap(1024, 800, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(errorBitmap);
                
                // 填充背景
                canvas.drawColor(Color.LTGRAY);
                
                // 绘制error文字
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setTextSize(40);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setAntiAlias(true);
                
                // 计算文字居中位置
                float x = errorBitmap.getWidth() / 2f;
                float y = errorBitmap.getHeight() / 2f - (paint.descent() + paint.ascent()) / 2f;
                canvas.drawText("error", x, y, paint);
                
                return errorBitmap;
            }
            
            options.inSampleSize = calculateInSampleSizeForRegion(rect, width, height);
            //Log.d("TAG", String.format("page:%s, w-h:%s-%s, region.w-h:%s-%s, patch:%s-%s, sample:%s, %s, rect:%s, %s",
            //        pageHandle, pageW, pageH, width, height, patchX, patchY, options.inSampleSize, pageSliceBounds, rect, path));
            Bitmap bitmap = null;
            try {
                bitmap = decoder.decodeRegion(rect, options);
            } catch (Exception e) {
                Log.d("TAG", String.format("错误:%s, w-h:%s-%s, 区域:w-h:%s-%s, region.w-h:%s-%s, patch:%s-%s, sample:%s, %s, rect:%s, %s",
                        pageHandle, pageWidth, pageHeight, pageW, pageH, width, height, patchX, patchY, options.inSampleSize, pageSliceBounds, rect, path));

                Log.e("TAG", String.format("decode.error:%s", e));
                bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565);
            }
            return bitmap;
        }
    }

    public Bitmap renderTifBitmap(Rect cropBound, int width, int height, RectF pageSliceBounds, float scale) {
        loadPage(0);

        if (pageHeight == 0 || pageWidth == 0) {
            decodeBound();
        }

        //缩略图
        if (pageSliceBounds.width() == 1.0f) {
            Bitmap bitmap = tiffLoader.decodeRegionToBitmap(
                    0,
                    0,
                    pageWidth,
                    pageHeight,
                    scale
            );
            Log.d("TAG", String.format("page:%s, page.w-h:%s-%s, scale:%s, w-h:%s-%s",
                    pageHandle, pageWidth, pageHeight, scale, width, height));
            return bitmap;
        } else {
            int pageW = (int) (width / scale);
            int pageH = (int) (height / scale);
            int patchX = Math.round(pageSliceBounds.left * pageWidth);
            int patchY = Math.round(pageSliceBounds.top * pageHeight);

            // 确保区域不超出边界
            //patchX = Math.max(0, Math.min(patchX, pageWidth));
            //patchY = Math.max(0, Math.min(patchY, pageHeight));

            int bitmapWidth = (int) (pageW * scale);
            int bitmapHeight = (int) (pageH * scale);

            Log.d("TAG", String.format("tiff.decode:%s, 原始w-h:%s-%s, :%s-%s, region.w-h:%s-%s, fixed:%s-%s, patch:%s-%s, scale:%s, %s",
                    pageHandle, pageWidth, pageHeight, pageW, pageH, width, height, bitmapWidth, bitmapHeight, patchX, patchY, scale, pageSliceBounds));
            Bitmap bitmap = null;
            try {
                bitmap = tiffLoader.decodeRegionToBitmap(
                        patchX,
                        patchY,
                        pageW,
                        pageH,
                        scale
                );
            } catch (Exception e) {
                Log.d("TAG", String.format("tiff错误:%s, w-h:%s-%s, region.w-h:%s-%s, patch:%s-%s, %s",
                        pageHandle, pageWidth, pageHeight, width, height, patchX, patchY, pageSliceBounds));

                Log.e("TAG", String.format("decode.tiff.error:%s", e));
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            }
            return bitmap;
        }
    }

    private int calculateInSampleSizeForRegion(
            android.graphics.Rect region,
            int reqWidth,
            int reqHeight
    ) {
        int regionWidth = region.width();
        int regionHeight = region.height();
        var inSampleSize = 1;

        if (regionHeight > reqHeight || regionWidth > reqWidth) {
            int halfHeight = regionHeight / 2;
            int halfWidth = regionWidth / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public List<Hyperlink> getPageLinks() {
        List<Hyperlink> hyperlinks = new ArrayList<>();

        return hyperlinks;
    }

    @Override
    public List<ReflowBean> getReflowBean() {
        return null;
    }

    public synchronized void recycle() {
        if (pageHandle >= 0) {
            pageHandle = -1;
        }
        if (decoder != null) {
            decoder.recycle();
        }
        if (tiffLoader != null) {
            tiffLoader.close();
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