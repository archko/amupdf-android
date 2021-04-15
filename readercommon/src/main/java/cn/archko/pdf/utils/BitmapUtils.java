package cn.archko.pdf.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Base64;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import cn.archko.pdf.common.Logcat;

/**
 * @author: archko 2019/2/21 :16:46
 */
public class BitmapUtils {

    public static boolean saveBitmapToFile(Bitmap bitmap, File file) {
        return saveBitmapToFile(bitmap, file, Bitmap.CompressFormat.PNG, 90);
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, File file, Bitmap.CompressFormat format, int quality) {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ByteArrayOutputStream baos = null;

        try {
            if (file.exists()) {
                file.delete();
            } else {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }

            baos = new ByteArrayOutputStream();
            bitmap.compress(format, quality, baos);
            byte[] byteArray = baos.toByteArray();// 字节数组输出流转换成字节数组
            // 将字节数组写入到刚创建的图片文件中
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(byteArray);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            StreamUtils.closeStream(baos);
            StreamUtils.closeStream(bos);
            StreamUtils.closeStream(fos);
        }
    }

    public static Bitmap base64ToBitmap(String str) {
        try {
            byte[] bytes = Base64.decode(str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static int i = 0;

    public static void saveBitmapToSDCard(Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/" + (i++) + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static Bitmap drawableToBitmap(Drawable drawable) // drawable 转换成 bitmap
    {
        int width = drawable.getIntrinsicWidth();   // 取 drawable 的长宽
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;         // 取 drawable 的颜色格式
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);     // 建立对应 bitmap
        Canvas canvas = new Canvas(bitmap);         // 建立对应 bitmap 的画布
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);      // 把 drawable 内容画到画布中
        return bitmap;
    }

    static Drawable zoomDrawable(Drawable drawable, int w, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap oldbmp = drawableToBitmap(drawable); // drawable 转换成 bitmap
        Matrix matrix = new Matrix();   // 创建操作图片用的 Matrix 对象
        float scaleWidth = ((float) w / width);   // 计算缩放比例
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);         // 设置缩放比例
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height, matrix, true);       // 建立新的 bitmap ，其内容是对原 bitmap 的缩放后的图
        return new BitmapDrawable(newbmp);       // 把 bitmap 转换成 drawable 并返回
    }

    static Drawable zoomDrawable(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();   // 创建操作图片用的 Matrix 对象
        float scaleWidth = ((float) w / width);   // 计算缩放比例
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);         // 设置缩放比例
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);       // 建立新的 bitmap ，其内容是对原 bitmap 的缩放后的图
        BitmapDrawable bitmapDrawable = new BitmapDrawable(newbmp);       // 把 bitmap 转换成 drawable 并返回
        bitmapDrawable.setBounds(0, 0, w, h);
        return bitmapDrawable;
    }

    private static final float zoom = 160f / 72;

    public static Drawable getDrawable(Bitmap bitmap, int screenWidth) {
        float width = bitmap.getWidth() * zoom;
        float height = bitmap.getHeight() * zoom;
        if (width > screenWidth) {
            float ratio = screenWidth / width;
            height = ratio * height;
            width = screenWidth;
        }
        Logcat.d(String.format("width:%s,height:%s,sc:%s", width, height, screenWidth));
        return zoomDrawable(bitmap, (int) width, (int) height);
    }

    public static Bitmap decodeFile(File file) {
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }
}
