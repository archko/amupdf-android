//package cn.archko.pdf.core.imagedroid;
//
//import android.content.ContentResolver;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.util.Log;
//
//import com.artifex.mupdf.fitz.Document;
//import com.artifex.mupdf.fitz.Matrix;
//import com.artifex.mupdf.fitz.Page;
//import com.artifex.mupdf.fitz.Rect;
//import com.artifex.mupdf.fitz.RectI;
//import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
//import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;
//
//import androidx.annotation.Keep;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import cn.archko.pdf.core.cache.BitmapPool;
//
//public class MupdfImageDecoder implements ImageDecoder {
//
//    public static final String TAG = "MupdfImageDecoder";
//    private static final String FILE_PREFIX = "file://";
//    private static final String ASSET_PREFIX = FILE_PREFIX + "/android_asset/";
//    private static final String RESOURCE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://";
//
//    Document document = null;
//
//    @Keep
//    public MupdfImageDecoder() {
//        this(null);
//    }
//
//    public MupdfImageDecoder(@Nullable Bitmap.Config bitmapConfig) {
//    }
//
//    @Override
//    @NonNull
//    public Bitmap decode(Context context, @NonNull Uri uri) throws Exception {
//        Bitmap bitmap = null;
//        String uriString = uri.toString();
//        if (uriString.startsWith(FILE_PREFIX)) {
//            String path = uriString.substring(FILE_PREFIX.length());
//            Log.e(TAG, "mupdf trying to open " + path);
//            try {
//                document = Document.openDocument(path);
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage());
//                return null;
//            }
//
//            bitmap = renderBitmap();
//        } else {
//            /*InputStream inputStream = null;
//            try {
//                ContentResolver contentResolver = context.getContentResolver();
//                inputStream = contentResolver.openInputStream(uri);
//            } finally {
//                if (inputStream != null) {
//                    try {
//                        inputStream.close();
//                    } catch (Exception e) {  }
//                }
//            }*/
//        }
//        if (bitmap == null) {
//            throw new RuntimeException("Mupdf image region decoder returned null bitmap - image format may not be supported");
//        }
//        return bitmap;
//    }
//
//    private Bitmap renderBitmap() {
//        Page page = document.loadPage(0);
//        Rect b = page.getBounds();
//        float width = (b.x1 - b.x0);
//        float height = (b.y1 - b.y0);
//        Bitmap bitmap = BitmapPool.getInstance().acquire((int) width, (int) height);
//        float zoom = 2f;
//        Matrix ctm = new Matrix(zoom, zoom);
//        RectI bbox = new RectI(page.getBounds().transform(ctm));
//        float xscale = width / (bbox.x1 - bbox.x0);
//        float yscale = height / (bbox.y1 - bbox.y0);
//        ctm.scale(xscale, yscale);
//        AndroidDrawDevice dev = new AndroidDrawDevice(bitmap, 0, 0, 0, 0,
//                bitmap.getWidth(), bitmap.getHeight());
//        page.run(dev, ctm, null);
//        page.destroy();
//        dev.close();
//        dev.destroy();
//        return bitmap;
//    }
//}
