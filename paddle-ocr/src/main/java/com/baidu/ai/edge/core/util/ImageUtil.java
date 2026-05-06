package com.baidu.ai.edge.core.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/ImageUtil.class */
public class ImageUtil {

    /* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/ImageUtil$ResizedBitmap.class */
    public static class ResizedBitmap {

        /* renamed from: a  reason: collision with root package name */
        Bitmap f25a;
        float b;
        float c;
        float d;

        public ResizedBitmap(Bitmap bitmap, float f, float f2, float f3) {
            this.f25a = bitmap;
            this.b = f2;
            this.c = f3;
            this.d = f;
        }

        public Bitmap getBitmap() {
            return this.f25a;
        }

        public float getResizedWidth() {
            return this.b;
        }

        public float getResizedHeight() {
            return this.c;
        }

        public float getScale() {
            return this.d;
        }
    }

    public static Bitmap resize(Bitmap bitmap, int i, int i2) {
        return Bitmap.createScaledBitmap(bitmap, i, i2, true);
    }

    public static Pair<Integer, Integer> calcTarget(Bitmap bitmap, int i, int i2) {
        int i3;
        int i4;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i("ImageUtil", "[preprocess] calcTarget wh" + bitmap.getWidth() + " " + bitmap.getHeight());
        if (width > height) {
            i3 = width;
            i4 = height;
        } else {
            i3 = height;
            i4 = width;
        }
        float f = 1.0f;
        if (i > 0) {
            f = (i * 1.0f) / i4;
        }
        float f2 = i3;
        float f3 = f * f2;
        float f4 = i2;
        if (f3 > f4) {
            f = (f4 * 1.0f) / f2;
        }
        return new Pair<>(Integer.valueOf(Math.round(f * width)), Integer.valueOf(Math.round(f * height)));
    }

    public static Pair<Integer, Integer> calcTargetSizeForKeepRatio2(Bitmap bitmap, int i, int i2) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i("ImageUtil", "[preprocess] calcTargetForKeepRatio2 " + bitmap.getWidth() + " * " + bitmap.getHeight());
        float f = width;
        float f2 = height;
        float min = Math.min((i * 1.0f) / f, (i2 * 1.0f) / f2);
        return new Pair<>(Integer.valueOf(Math.round(min * f)), Integer.valueOf(Math.round(min * f2)));
    }

    public static Pair<Bitmap, Float> resizeTarget(Bitmap bitmap, int i, int i2) {
        int i3;
        int i4;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i("ImageUtil", "[preprocess] origin bitmap wh" + bitmap.getWidth() + " " + bitmap.getHeight());
        if (width > height) {
            i3 = width;
            i4 = height;
        } else {
            i3 = height;
            i4 = width;
        }
        float f = 1.0f;
        if (i > 0) {
            f = (i * 1.0f) / i4;
        }
        float f2 = i3;
        float f3 = f * f2;
        float f4 = i2;
        if (f3 > f4) {
            f = (f4 * 1.0f) / f2;
        }
        Bitmap resize = resize(bitmap, Math.round(f * width), Math.round(f * height));
        Log.i("ImageUtil", "[preprocess] scale :" + f + " ; maxSize" + i2 + " ; targetSize" + i);
        return new Pair<>(resize, Float.valueOf(f));
    }

    public static int calPaddedValue(int i, int i2) {
        int i3;
        if (i2 != 0 && (i3 = i % i2) != 0) {
            return i + i3;
        }
        return i;
    }

    public static Pair<Integer, Integer> calcShrinkSize(int i, int i2) {
        double a2 = a(i, i2);
        return new Pair<>(Integer.valueOf(Double.valueOf(a2 * i).intValue()), Integer.valueOf(Double.valueOf(a2 * i2).intValue()));
    }

    public static Pair<Integer, Integer> calcWithStep(Bitmap bitmap, int i, int i2) {
        int width = bitmap.getWidth();
        int i3 = width;
        int height = bitmap.getHeight();
        int i4 = height;
        int max = Math.max(width, height);
        if (max > i) {
            float f = (i * 1.0f) / max;
            i3 = (int) Math.floor(f * i3);
            i4 = (int) Math.floor(f * i4);
        }
        int i5 = i3;
        int i6 = i5 - (i5 % i2);
        int i7 = i6;
        if (i6 == 0) {
            i7 = i2;
        }
        int i8 = i4;
        int i9 = i8 - (i8 % i2);
        int i10 = i9;
        if (i9 == 0) {
            i10 = i2;
        }
        return new Pair<>(Integer.valueOf(i7), Integer.valueOf(i10));
    }

    public static Bitmap resizeWithStep(Bitmap bitmap, int i, int i2) {
        int width = bitmap.getWidth();
        int i3 = width;
        int height = bitmap.getHeight();
        int i4 = height;
        int max = Math.max(width, height);
        if (max > i) {
            float f = (i * 1.0f) / max;
            i3 = (int) Math.floor(f * i3);
            i4 = (int) Math.floor(f * i4);
        }
        int i5 = i3;
        int i6 = i5 - (i5 % i2);
        int i7 = i6;
        if (i6 == 0) {
            i7 = i2;
        }
        int i8 = i4;
        int i9 = i8 - (i8 % i2);
        int i10 = i9;
        if (i9 == 0) {
            i10 = i2;
        }
        return resize(bitmap, i7, i10);
    }

    private static double a(int i, int i2) {
        double d = i2 * i;
        double pow = Math.pow(3721808.746967071d / d, 0.5d);
        double pow2 = Math.pow(2777088.0d / d, 0.5d);
        System.out.println("maxShrinkV1:" + pow + "   " + pow);
        double intValue = (Double.valueOf(Math.min(pow, pow2) * 100.0d).intValue() - 30) / 100.0d;
        double d2 = intValue;
        System.out.println("maxShrink double:" + d2);
        if (intValue >= 5.0d) {
            d2 -= 0.5d;
        } else if (d2 < 5.0d && d2 >= 4.0d) {
            d2 -= 0.4d;
        } else if (d2 < 4.0d && d2 >= 3.0d) {
            d2 -= 0.3d;
        } else if (d2 < 3.0d && d2 >= 2.0d) {
            d2 -= 0.2d;
        } else if (d2 < 2.0d && d2 >= 1.5d) {
            d2 -= 0.1d;
        } else if (d2 < 1.5d && d2 >= 0.75d) {
            d2 = 0.5d;
        } else if (d2 < 0.75d && d2 > 0.1d) {
            d2 *= 0.5d;
        } else if (d2 <= 0.1d) {
            d2 = 0.1d;
        }
        if (d2 >= 1.0d) {
            d2 = 1.0d;
        }
        double d3 = d2;
        System.out.println("shrink:" + d2);
        return d3;
    }

    public static float[] getPixels(Bitmap bitmap, float[] fArr, float[] fArr2, boolean z, boolean z2) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[] fArr3 = new float[width * 3 * height];
        if (z) {
            int i = 0;
            for (int i2 = 0; i2 < height; i2++) {
                for (int i3 = 0; i3 < width; i3++) {
                    int pixel = bitmap.getPixel(i3, i2);
                    fArr3[i] = z2 ? Color.red(pixel) : Color.blue(pixel);
                    fArr3[i] = (fArr3[i] - fArr[0]) * fArr2[0];
                    int i4 = i + 1;
                    fArr3[i4] = Color.green(pixel);
                    fArr3[i4] = (fArr3[i4] - fArr[1]) * fArr2[1];
                    int i5 = i + 2;
                    fArr3[i5] = z2 ? Color.blue(pixel) : Color.red(pixel);
                    fArr3[i5] = (fArr3[i5] - fArr[2]) * fArr2[2];
                    i += 3;
                }
            }
        } else {
            for (int i6 = 0; i6 < height; i6++) {
                for (int i7 = 0; i7 < width; i7++) {
                    int i8 = i6;
                    int i9 = (i8 * width) + i7;
                    int i10 = width * height;
                    int i11 = i9 + i10;
                    int i12 = i11 + i10;
                    int pixel2 = bitmap.getPixel(i7, i8);
                    if (z2) {
                        fArr3[i9] = (Color.red(pixel2) - fArr[0]) * fArr2[0];
                        fArr3[i11] = (Color.green(pixel2) - fArr[1]) * fArr2[1];
                        fArr3[i12] = (Color.blue(pixel2) - fArr[2]) * fArr2[2];
                    } else {
                        fArr3[i9] = (Color.blue(pixel2) - fArr[0]) * fArr2[0];
                        fArr3[i11] = (Color.green(pixel2) - fArr[1]) * fArr2[1];
                        fArr3[i12] = (Color.red(pixel2) - fArr[2]) * fArr2[2];
                    }
                }
            }
        }
        return fArr3;
    }
}
