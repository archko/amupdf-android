package cn.archko.pdf.core.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * @author: archko 2024/2/14 :19:13
 */
public class CropUtils {

    /*public static RectF getNativeCropRect(Bitmap bitmap) {
        //long start = SystemClock.uptimeMillis();
        ByteBuffer byteBuffer = PageCropper.create(bitmap.getByteCount()).order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        //Log.d("test", String.format("%s,%s,%s,%s", bitmap.getWidth(), bitmap.getHeight(), (SystemClock.uptimeMillis() - start), rectF));

        //view: view:Point(1920, 1080) patchX:71 mss:6.260591 mZoomSize:Point(2063, 3066) zoom:1.0749608
        //test: 2063,3066,261,RectF(85.0, 320.0, 1743.0, 2736.0)
        return PageCropper.getCropBounds(byteBuffer, bitmap.getWidth(), bitmap.getHeight(), new RectF(0f, 0f, bitmap.getWidth(), bitmap.getHeight()));
    }*/

    /**
     * 灰度化 bitmap
     *
     * @param width
     * @param height
     * @param pixels
     * @return
     */
    private static void setGray(Bitmap bitmap, int width, int height, int[] pixels) {
        int alpha = 0xFF << 24;  //设置透明度
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int gray = pixels[width * i + j];
                int red = ((gray & 0x00FF0000) >> 16);  //获取红色灰度值
                int green = ((gray & 0x0000FF00) >> 8); //获取绿色灰度值
                int blue = (gray & 0x000000FF);         //获取蓝色灰度值
                gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);

                if (gray < 180) {//如果某点像素灰度小于给定阈值,180的时候,测试页面都正常切边了.
                    gray = 0;//将该点像素的灰度值置为0（黑色）
                } else {//如果某点像素灰度大于或等于给定阈值
                    gray = 255;//将该点像素的灰度值置为1（白色）
                }

                gray = alpha | (gray << 16) | (gray << 8) | gray; //添加透明度
                pixels[width * i + j] = gray;   //更改像素色值
            }
        }
        //Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    /**
     * min width/height,if a bitmap is to small, don't crop
     */
    public static final int MIN_WIDTH = 30;
    private static final int THRESHOLD = 4;

    /**
     * 得到切除白边的rect.
     * 对于黑白的比较精确,但效率低,因为全图扫描.
     *
     * @param bitmap
     * @return
     */
    public static RectF getJavaCropRect(Bitmap bitmap) {
        if (bitmap.getHeight() < (MIN_WIDTH) || bitmap.getWidth() < MIN_WIDTH) {
            return new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        long start = System.currentTimeMillis();
        int[] pixels = getPixels(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));

        // 灰度化 bitmap
        setGray(bitmap,
                bitmap.getWidth(),
                bitmap.getHeight(),
                pixels);

        int left = 0; // 左边框白色高度
        int top = 0;  // 上边框白色高度
        int right = 0; // 右边框白色高度
        int bottom = 0; // 底边框白色高度

        left = getLeft(bitmap);

        top = getTop(bitmap);

        right = getRight(bitmap);

        bottom = getBottom(bitmap);

        if (top > THRESHOLD) {
            top -= THRESHOLD;
        }
        if (left > THRESHOLD) {
            left -= THRESHOLD;
        }
        if (right > THRESHOLD) {
            right -= THRESHOLD;
        }
        if (bottom > THRESHOLD) {
            bottom -= THRESHOLD;
        }

        System.out.println(String.format("crop-time:%s", (System.currentTimeMillis() - start)));
        return new RectF((float) left, (float) top, bitmap.getWidth() - right, bitmap.getHeight() - bottom);
    }

    private static int getLeft(Bitmap bitmap) {
        int left = 0;
        int w = bitmap.getWidth() / 3;
        for (int x = 0; x < w; x++) {
            boolean holdBlackPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                if (bitmap.getPixel(x, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            left++;
        }
        return left;
    }

    private static int getTop(Bitmap bitmap) {
        int top = 0;
        int h = bitmap.getHeight() / 3;
        for (int y = 0; y < h; y++) {
            boolean holdBlackPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                int pixel = bitmap.getPixel(w, y);
                if (pixel != -1) { // -1 是白色
                    holdBlackPix = true; // 如果不是-1 则是其他颜色
                    break;
                }
            }

            if (holdBlackPix) {
                break;
            }
            top++;
        }
        return top;
    }

    private static int getRight(Bitmap bitmap) {
        int right = 0;
        int w = bitmap.getWidth() * 2 / 3;
        for (int x = bitmap.getWidth() - 1; x >= w; x--) {
            boolean holdBlackPix = false;
            for (int h = 0; h < bitmap.getHeight(); h++) {
                if (bitmap.getPixel(x, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            right++;
        }
        return right;
    }

    private static int getBottom(Bitmap bitmap) {
        int bottom = 0;
        int h = bitmap.getHeight() * 2 / 3;
        for (int y = bitmap.getHeight() - 1; y >= h; y--) {
            boolean holdBlackPix = false;
            for (int w = 0; w < bitmap.getWidth(); w++) {
                if (bitmap.getPixel(w, y) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            bottom++;
        }
        return bottom;
    }

    //=======================
    //========================= java =========================

    // 扫描步进
    private final static int LINE_SIZE = 4;
    //边距留白,与切割的图片大小是有关的,缩略图越小,留白应该越小,因为精度小
    private final static int LINE_MARGIN = 8;
    //这个值越小,表示忽略的空间越大,比如一行就一个页码,如果这个适中,就直接忽略,认为这行是空白的
    private final static double WHITE_THRESHOLD = 0.004;

    /**
     * 使用的是平均像素,对于有些图片切割会异常,在于精确度的调整.
     * 扫描步进为5,平均200*200的图片1毫秒内,对于红色字,会当成无效的字
     *
     * @param bitmap
     * @param bitmapBounds
     * @return
     */
    public static RectF getJavaCropBounds(final Bitmap bitmap) {
        return getJavaCropBounds(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
    }

    public static RectF getJavaCropBounds(final Bitmap bitmap, final Rect bitmapBounds) {
        if (bitmap.getHeight() < (MIN_WIDTH) || bitmap.getWidth() < MIN_WIDTH) {
            return new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        //long start = System.currentTimeMillis();

        //计算平均灰度不如直接设置225,效果要好的多,平均值会把红色的识别成白边
        final float avgLum = 225; //calculateAvgLum(bitmap, bitmapBounds);
        final float rightBottomAvgLum = 235; //calculateAvgLum(bitmap, bitmapBounds);
        float left = getLeftBound(bitmap, bitmapBounds, avgLum);
        float right = getRightBound(bitmap, bitmapBounds, rightBottomAvgLum);
        float top = getTopBound(bitmap, bitmapBounds, avgLum);
        float bottom = getBottomBound(bitmap, bitmapBounds, rightBottomAvgLum);

        left = left * bitmapBounds.width();
        top = top * bitmapBounds.height();
        right = right * bitmapBounds.width();
        bottom = bottom * bitmapBounds.height();

        //System.out.println(String.format("droid-crop-time:%s", (System.currentTimeMillis() - start)));
        return new RectF(left, top, right, bottom);
    }

    private static float getLeftBound(final Bitmap bmp, final Rect bitmapBounds, final float avgLum) {
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        for (x = bitmapBounds.left; x < bitmapBounds.left + w; x += LINE_SIZE) {
            final boolean white = isRectWhite(bmp, x, bitmapBounds.top + LINE_MARGIN, x + LINE_SIZE, bitmapBounds.bottom - LINE_MARGIN,
                    avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(bitmapBounds.left, x - LINE_SIZE) - bitmapBounds.left)
                            / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(bitmapBounds.left, x - LINE_SIZE) - bitmapBounds.left)
                / bitmapBounds.width() : 0;
    }

    private static float getTopBound(final Bitmap bmp, final Rect bitmapBounds, final float avgLum) {
        final int h = bmp.getHeight() / 3;
        int whiteCount = 0;
        int y = 0;
        for (y = bitmapBounds.top; y < bitmapBounds.top + h; y += LINE_SIZE) {
            final boolean white = isRectWhite(bmp, bitmapBounds.left + LINE_MARGIN, y, bitmapBounds.right - LINE_MARGIN, y + LINE_SIZE,
                    avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.max(bitmapBounds.top, y - LINE_SIZE) - bitmapBounds.top)
                            / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.max(bitmapBounds.top, y - LINE_SIZE) - bitmapBounds.top)
                / bitmapBounds.height() : 0;
    }

    private static float getRightBound(final Bitmap bmp, final Rect bitmapBounds, final float avgLum) {
        final int w = bmp.getWidth() / 3;
        int whiteCount = 0;
        int x = 0;
        for (x = bitmapBounds.right - LINE_SIZE; x > bitmapBounds.right - w; x -= LINE_SIZE) {
            final boolean white = isRectWhite(bmp, x, bitmapBounds.top + LINE_MARGIN, x + LINE_SIZE, bitmapBounds.bottom - LINE_MARGIN,
                    avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(bitmapBounds.right, x + 2 * LINE_SIZE) - bitmapBounds.left)
                            / bitmapBounds.width();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(bitmapBounds.right, x + 2 * LINE_SIZE) - bitmapBounds.left)
                / bitmapBounds.width() : 1;
    }

    private static float getBottomBound(final Bitmap bmp, final Rect bitmapBounds, final float avgLum) {
        final int h = bmp.getHeight() * 2 / 3;
        int whiteCount = 0;
        int y = 0;
        for (y = bitmapBounds.bottom - LINE_SIZE; y > bitmapBounds.bottom - h; y -= LINE_SIZE) {
            final boolean white = isRectWhite(bmp, bitmapBounds.left + LINE_MARGIN, y, bitmapBounds.right - LINE_MARGIN, y + LINE_SIZE,
                    avgLum);
            if (white) {
                whiteCount++;
            } else {
                if (whiteCount >= 1) {
                    return (float) (Math.min(bitmapBounds.bottom, y + 2 * LINE_SIZE) - bitmapBounds.top)
                            / bitmapBounds.height();
                }
                whiteCount = 0;
            }
        }
        return whiteCount > 0 ? (float) (Math.min(bitmapBounds.bottom, y + 2 * LINE_SIZE) - bitmapBounds.top)
                / bitmapBounds.height() : 1;
    }

    private static boolean isRectWhite(final Bitmap bmp, final int l, final int t, final int r, final int b,
                                       final float avgLum) {
        int count = 0;

        final int[] pixels = getPixels(bmp, new Rect(l, t, r, b));
        for (final int c : pixels) {
            final float lum = getLum(c);
            if ((lum < avgLum) && ((avgLum - lum) * 10 > avgLum)) {
                count++;
            }
        }
        return ((float) count / pixels.length) < WHITE_THRESHOLD;
    }

    private static float calculateAvgLum(final Bitmap bmp, final Rect bitmapBounds) {
        if (bmp == null) {
            return 1000;
        }

        float lum = 0f;

        final int sizeX = bitmapBounds.width() / 10;
        final int sizeY = bitmapBounds.height() / 10;
        final int centerX = bitmapBounds.centerX();
        final int centerY = bitmapBounds.centerX();

        final int[] pixels = getPixels(bmp,
                new Rect(centerX - sizeX,
                        centerY - sizeY,
                        centerX + sizeX,
                        centerY + sizeY));
        for (final int c : pixels) {
            lum += getLum(c);
        }

        return lum / (pixels.length);
    }

    private static float getLum(final int c) {
        final int r = (c & 0xFF0000) >> 16;
        final int g = (c & 0xFF00) >> 8;
        final int b = c & 0xFF;

        final int min = Math.min(r, Math.min(g, b));
        final int max = Math.max(r, Math.max(g, b));
        return (1f * min + max) / 2;
    }

    public static int[] getPixels(Bitmap bitmap, Rect srcRect) {
        int width = srcRect.width();
        int height = srcRect.height();
        //boolean hasAlpha = bitmap.hasAlpha();
        int[] pixels = new int[width * height];

        bitmap.getPixels(pixels, 0, width, srcRect.left, srcRect.top, width, height);
        return pixels;
    }
}
