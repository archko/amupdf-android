package cn.archko.pdf.common;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * @author: archko 2019/12/25 :15:54
 */
public class BitmapCache {

    public static BitmapCache getInstance() {
        return Factory.instance;
    }

    private static final class Factory {

        private static final BitmapCache instance = new BitmapCache();
    }

    private BitmapCache() {
    }

    /**
     * cache size for amupdf
     */
    public static final int CAPACITY_FOR_AMUPDF = 16;
    /**
     * cache size for vudroid
     */
    public static final int CAPACITY_FOR_VUDROID = 32;

    private int capacity = 8;
    private LruCache<Object, Bitmap> cacheKt = new RecycleLruCache(capacity);

    public LruCache<Object, Bitmap> getCache() {
        return cacheKt;
    }

    public void resize(int maxSize) {
        capacity = maxSize;
        cacheKt.resize(maxSize);
    }

    public void clear() {
        cacheKt.evictAll();
    }

    public void addBitmap(Object key, Bitmap val) {
        cacheKt.put(key, val);
    }

    public Bitmap getBitmap(Object key) {
        return cacheKt.get(key);
    }

    public Bitmap removeBitmap(Object key) {
        Bitmap bitmap = cacheKt.get(key);
        cacheKt.remove(key);
        return bitmap;
    }

    private static class RecycleLruCache extends LruCache<Object, Bitmap> {

        public RecycleLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, @NonNull Object key, @NonNull Bitmap oldValue, @Nullable Bitmap newValue) {
            //BitmapPool.getInstance().release(oldValue);
        }
    }
}
