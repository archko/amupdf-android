package cn.archko.pdf.core.imagedroid;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import java.io.File;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.common.AppExecutors;

public class MupdfPooledImageRegionDecoder implements ImageRegionDecoder {

    private static final String TAG = MupdfPooledImageRegionDecoder.class.getSimpleName();

    private static boolean debug = false;
    private static final String FILE_PREFIX = "file://";
    private static final String ASSET_PREFIX = FILE_PREFIX + "/android_asset/";
    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
    private static final int SHOW_LOADING_SIZE = 10_000_000;

    private Context context;
    private Uri uri;

    private long fileLength = Long.MAX_VALUE;
    private final Point imageDimensions = new Point(0, 0);
    private ProgressDialog progressDialog;

    Document decoder = null;
    Page page = null;

    @Keep
    public MupdfPooledImageRegionDecoder() {
        this(null);
    }

    public MupdfPooledImageRegionDecoder(@Nullable Bitmap.Config bitmapConfig) {
    }

    @Keep
    public static void setDebug(boolean debug) {
        MupdfPooledImageRegionDecoder.debug = debug;
    }

    @Override
    @NonNull
    public Point init(final Context context, @NonNull final Uri uri) throws Exception {
        this.context = context;
        this.uri = uri;
        initialiseDecoder();
        return this.imageDimensions;
    }

    private void initialiseDecoder() {
        String uriString = uri.toString();

        long fileLength = Long.MAX_VALUE;
        if (uriString.startsWith(FILE_PREFIX)) {
            String path = uriString.substring(FILE_PREFIX.length());

            File file = new File(path);
            if (file.exists()) {
                fileLength = file.length();
            }

            debug("mupdf trying to open " + path);
            try {
                decoder = Document.openDocument(path);
            } catch (Exception e) {
                debug(e.getMessage());
                return;
            }
        } else {
            /*InputStream inputStream = null;
            try {
                ContentResolver contentResolver = context.getContentResolver();
                inputStream = contentResolver.openInputStream(uri);
                try {
                    AssetFileDescriptor descriptor = contentResolver.openAssetFileDescriptor(uri, "r");
                    if (descriptor != null) {
                        fileLength = descriptor.getLength();
                    }
                } catch (Exception e) {
                    // Stick with MAX_LENGTH
                }
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {   }
                }
            }*/
        }

        if (fileLength > SHOW_LOADING_SIZE) {
            showLoading();
        }

        this.fileLength = fileLength;
        page = decoder.loadPage(0);
        Rect b = page.getBounds();
        int width = (int) (b.x1 - b.x0);
        int height = (int) (b.y1 - b.y0);
        this.imageDimensions.set(width, height);
        hideLoading();
    }

    private void showLoading() {
        AppExecutors.Companion.getInstance().mainThread().execute(() -> {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Waiting...");
            progressDialog.show();
        });
    }

    private void hideLoading() {
        AppExecutors.Companion.getInstance().mainThread().execute(() -> {
            if (progressDialog != null) {
                progressDialog.hide();
            }
        });
    }

    @Override
    @NonNull
    public Bitmap decodeRegion(@NonNull android.graphics.Rect sRect, int sampleSize) {
        debug("Decode region " + sRect + " on thread " + Thread.currentThread().getName());
        if (sRect.width() < imageDimensions.x || sRect.height() < imageDimensions.y) {
            if (null == decoder) {
                try {
                    initialiseDecoder();
                } catch (Exception e) {
                }
            }
        }

        try {
            if (decoder != null) {
                Bitmap bitmap = renderBitmap(sRect, sampleSize);
                if (bitmap == null) {
                    throw new RuntimeException("Mupdf image decoder returned null bitmap - image format may not be supported");
                }
                return bitmap;
            }
        } catch (Exception e) {
            debug(e.getMessage());
        }
        return null;
    }

    public Bitmap renderBitmap(android.graphics.Rect cropBound, int sampleSize) {
        float scale = 1f / sampleSize;
        int pageW;
        int pageH;
        int patchX;
        int patchY;
        //如果页面的缩放为1,那么这时的pageW就是view的宽.
        pageW = (int) (cropBound.width() * scale);
        pageH = (int) (cropBound.height() * scale);

        patchX = (int) (cropBound.left * scale);
        patchY = (int) (cropBound.top * scale);
        Bitmap bitmap = BitmapPool.getInstance().acquire(pageW, pageH);
        if (null == page) {
            page = decoder.loadPage(0);
        }
        com.artifex.mupdf.fitz.Matrix ctm = new com.artifex.mupdf.fitz.Matrix(scale);
        AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, patchX, patchY, 0, 0, pageW, pageH);

        try {
            page.run(dev, ctm, null);
        } catch (Exception e) {
            debug(e.getMessage());
        }
        dev.close();
        dev.destroy();

        return bitmap;
    }

    @Override
    public synchronized boolean isReady() {
        return decoder != null /*&& page != null*/;
    }

    @Override
    public synchronized void recycle() {
        if (null != page) {
            page.destroy();
            page = null;
        }
        if (decoder != null) {
            decoder.destroy();
            context = null;
            uri = null;
        }
    }

    private void debug(String message) {
        if (debug) {
            Log.d(TAG, message);
        }
    }

}
