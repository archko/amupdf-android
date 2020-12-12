package cn.archko.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import cn.archko.pdf.App;

/**
 * @author: archko 2014/4/17 :15:21
 */
public class Utils {

    public static final String getFileSize(final long size) {
        if (size > 1073741824) {
            return String.format("%.2f", size / 1073741824.0) + " GB";
        } else if (size > 1048576) {
            return String.format("%.2f", size / 1048576.0) + " MB";
        } else if (size > 1024) {
            return String.format("%.2f", size / 1024.0) + " KB";
        } else {
            return size + " B";
        }

    }

    /**
     * Execute an {@link android.os.AsyncTask} on a thread pool
     *
     * @param forceSerial True to force the task to run in serial order
     * @param task        Task to execute
     * @param args        Optional arguments to pass to
     *                    {@link android.os.AsyncTask#execute(Object[])}
     * @param <T>         Task argument type
     */
    @SuppressLint("NewApi")
    public static <T> void execute(final boolean forceSerial, final AsyncTask<T, ?, ?> task,
                                   final T... args) {
        final WeakReference<AsyncTask<T, ?, ?>> taskReference = new WeakReference<AsyncTask<T, ?, ?>>(
                task);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.DONUT) {
            throw new UnsupportedOperationException(
                    "This class can only be used on API 4 and newer.");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || forceSerial) {
            taskReference.get().execute(args);
        } else {
            taskReference.get().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
        }
    }

    /**
     * @param context
     * @return
     */
    public static int getScreenWidthPixelWithOrientation(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        int width = (int) dm.widthPixels;
        return width;
    }

    public static int getScreenHeightPixelWithOrientation(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        int width = (int) dm.heightPixels;
        return width;
    }

    public static void saveBitmap(Bitmap bitmap) {
        Bitmap b = Bitmap.createBitmap(bitmap);
        Canvas canvas = new Canvas();
        canvas.drawBitmap(b, 0, 0, null);
        try {
            b.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + File.separator + "--" + SystemClock.uptimeMillis() + ".jpg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int parseInt(String source) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static float getScale() {
        return App.Companion.getInstance().getResources().getDisplayMetrics().density;
    }

    /**
     * 根据手机的分辨率从 dip 的单位 转成为 pixel(像素)
     */
    public static int dipToPixel(float dip) {
        float scale = App.Companion.getInstance().getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    /**
     * 解决不能layout不能预览的问题
     * 根据手机的分辨率从 dip 的单位 转成为 pixel(像素)
     */
    public static int dipToPixel(Context context, float dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 pixel(像素) 的单位 转成为 dip
     */
    public static int pixelToDip(float px) {
        float scale = App.Companion.getInstance().getResources().getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue
     * @return
     */
    public static int px2sp(float pxValue) {
        final float fontScale = App.Companion.getInstance().getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @return
     */
    public static int sp2px(float spValue) {
        final float fontScale = App.Companion.getInstance().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != App.uiThread) {
            new Handler(Looper.getMainLooper()).post(action);
        } else {
            action.run();
        }
    }
}
