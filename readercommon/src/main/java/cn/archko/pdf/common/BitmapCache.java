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
        pageCache = new InnerCache(MAX_PAGE_SIZE_IN_BYTES);
        nodeCache = new InnerCache(MAX_NODE_SIZE_IN_BYTES);
    }

    public static void setMaxMemory(float maxMemory) {
        MAX_PAGE_SIZE_IN_BYTES = (int) (maxMemory * 0.75);
        MAX_NODE_SIZE_IN_BYTES = (int) (maxMemory - MAX_PAGE_SIZE_IN_BYTES);
    }

    private InnerCache pageCache;
    private InnerCache nodeCache;

    public final Bitmap getBitmap(@NonNull String key) {
        return pageCache.getBitmap(key);
    }

    @Nullable
    public final Bitmap addBitmap(@NonNull String key, @NonNull Bitmap value) {
        return pageCache.addBitmap(key, value);
    }

    public final Bitmap remove(@NonNull String key) {
        return pageCache.remove(key);
    }

    public final Bitmap getNodeBitmap(@NonNull String key) {
        return nodeCache.getBitmap(key);
    }

    @Nullable
    public final Bitmap addNodeBitmap(@NonNull String key, @NonNull Bitmap value) {
        return nodeCache.addBitmap(key, value);
    }

    public final Bitmap removeNode(@NonNull String key) {
        return nodeCache.remove(key);
    }

    public final void clear() {
        pageCache.clear();
        nodeCache.clear();
    }

    /**
     * 页面缩略图的缓存大小,通常按页面高宽的1/4,如果页面非常大,比如4000,那么缓存能存10多屏
     */
    private static int MAX_PAGE_SIZE_IN_BYTES = 160 * 1024 * 1024;
    /**
     * 节点的缓存,平均一个1080*2240的屏幕上的node需要2419200*4,大约8mb多点,32m可以缓存几个屏幕
     */
    private static int MAX_NODE_SIZE_IN_BYTES = 32 * 1024 * 1024;

    private static class InnerCache {
        private int maxByte = MAX_PAGE_SIZE_IN_BYTES;
        private int mPoolSizeInBytes = 0;

        private final LinkedHashMap<String, Bitmap> bitmapLinkedMap = new LinkedHashMap<>(16, 0.75f, true);

        private int putCount;
        private int createCount;
        private int evictionCount;
        private int hitCount;
        private int missCount;

        public InnerCache(int maxByte) {
            this.maxByte = maxByte;
        }

        @Nullable
        public final Bitmap getBitmap(@NonNull String key) {
            if (key == null) {
                throw new NullPointerException("key == null");
            }

            Bitmap mapValue;
            synchronized (this) {
                mapValue = bitmapLinkedMap.get(key);
                if (mapValue != null) {
                    hitCount++;
                    if (mapValue.isRecycled()) {
                        bitmapLinkedMap.remove(key);
                        return null;
                    }
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
            while (mPoolSizeInBytes > MAX_PAGE_SIZE_IN_BYTES) {
                removeLast();
            }

            mPoolSizeInBytes += value.getByteCount();

            Bitmap previous;
            synchronized (this) {
                putCount++;
                previous = bitmapLinkedMap.put(key, value);
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
                if (bitmapLinkedMap.isEmpty()) {
                    return;
                }

                Map.Entry<String, Bitmap> toEvict = bitmapLinkedMap.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                bitmapLinkedMap.remove(key);
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
                previous = bitmapLinkedMap.remove(key);
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
            int size = bitmapLinkedMap.size();
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
            return new LinkedHashMap<String, Bitmap>(bitmapLinkedMap);
        }
    }
}
