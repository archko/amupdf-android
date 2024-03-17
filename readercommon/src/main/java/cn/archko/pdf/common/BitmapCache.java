package cn.archko.pdf.common;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final int mMaxPoolSizeInBytes = 160 * 1024 * 1024;

    private int mPoolSizeInBytes = 0;

    private final LinkedHashMap<String, Bitmap> map = new LinkedHashMap<>(16, 0.75f, true);

    private int putCount;
    private int createCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;

    @Nullable
    public final Bitmap getBitmap(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;
                return mapValue;
            }
            missCount++;
        }

        /*Bitmap createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                //size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }*/
        return null;
    }

    @Nullable
    public final Bitmap addBitmap(@NonNull String key, @NonNull Bitmap value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }
        while (mPoolSizeInBytes > mMaxPoolSizeInBytes) {
            removeLast();
        }

        mPoolSizeInBytes += value.getByteCount();

        Bitmap previous;
        synchronized (this) {
            putCount++;
            previous = map.put(key, value);
            if (previous != null) {
                mPoolSizeInBytes -= previous.getByteCount();
            }
        }
        //System.out.println(String.format("put.size:%s, key:%s, val:%s, size:%s", map.size(), key, value, mPoolSizeInBytes));

        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        return previous;
    }

    private void removeLast() {
        String key;
        Bitmap value;
        synchronized (this) {
            if (map.isEmpty()) {
                return;
            }

            Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
            key = toEvict.getKey();
            value = toEvict.getValue();
            map.remove(key);
            mPoolSizeInBytes -= value.getByteCount();
            evictionCount++;
        }
        //System.out.println(String.format("removeLast.size:%s, key:%s,val:%s, size:%s", map.size(), key, value, mPoolSizeInBytes));

        entryRemoved(true, key, value, null);
    }

    @Nullable
    public final Bitmap remove(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                mPoolSizeInBytes -= previous.getByteCount();
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, null);
        }

        return previous;
    }

    protected void entryRemoved(boolean evicted, @NonNull String key, @NonNull Bitmap oldValue,
                                @Nullable Bitmap newValue) {
    }

    @Nullable
    protected Bitmap create(@NonNull String key) {
        return null;
    }

    public final void clear() {
        int size = map.size();
        for (int i = 0; i < size; i++) {
            removeLast();
        }
    }

    public synchronized final int hitCount() {
        return hitCount;
    }

    public synchronized final int missCount() {
        return missCount;
    }

    public synchronized final int createCount() {
        return createCount;
    }

    public synchronized final int putCount() {
        return putCount;
    }

    public synchronized final int evictionCount() {
        return evictionCount;
    }

    public synchronized final Map<String, Bitmap> snapshot() {
        return new LinkedHashMap<String, Bitmap>(map);
    }

}
