package org.ebookdroid.core.crop;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;

import java.nio.ByteBuffer;

public class PageCropper {

    private PageCropper() {
    }
    //========================= native crop =========================

    static {
        System.loadLibrary("crop-lib");
    }

    public static native ByteBuffer create(int size);

    public static synchronized RectF getCropBounds(final ByteBuffer pixels, int width, int height, final RectF psb) {
        return nativeGetCropBounds(pixels, width, height, psb.left, psb.top, psb.right, psb.bottom);
    }

    /*public static synchronized RectF getColumn(final ByteBufferBitmap bitmap, final float x, final float y) {
        return nativeGetColumn(bitmap.getPixels(), bitmap.getWidth(), bitmap.getHeight(), x, y);
    }*/

    private static native RectF nativeGetCropBounds(ByteBuffer pixels, int width, int height, float left, float top,
                                                    float right, float bottom);

    //private static native RectF nativeGetColumn(ByteBuffer pixels, int width, int height, float x, float y);

    //========================= java =========================

    private final static int LINE_SIZE = 5;
    private final static int LINE_MARGIN = 20;
    private final static double WHITE_THRESHOLD = 0.005;

    public static RectF getCropBounds(final Bitmap bitmap, final Rect bitmapBounds, final RectF pageSliceBounds) {
        // final long t0 = System.currentTimeMillis();
        final float avgLum = calculateAvgLum(bitmap, bitmapBounds);
        // final long t1 = System.currentTimeMillis();
        final float left = getLeftBound(bitmap, bitmapBounds, avgLum);
        final float right = getRightBound(bitmap, bitmapBounds, avgLum);
        final float top = getTopBound(bitmap, bitmapBounds, avgLum);
        final float bottom = getBottomBound(bitmap, bitmapBounds, avgLum);
        // final long t5 = System.currentTimeMillis();

        // System.out.println("Crop: total=" + (t5 - t0) + "ms, avgLum=" + (t1 - t0) + "ms");

        return new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top * pageSliceBounds.height()
                + pageSliceBounds.top, right * pageSliceBounds.width() + pageSliceBounds.left, bottom
                * pageSliceBounds.height() + pageSliceBounds.top);
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
        return (min + max) / 2;
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
