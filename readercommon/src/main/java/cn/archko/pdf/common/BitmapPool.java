package cn.archko.pdf.common;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

/**
 * Created by archko on 16/12/24.
 */

public class BitmapPool {

    private static BitmapPool sInstance = new BitmapPool();
    private FixedSimplePool<Bitmap> simplePool;

    private BitmapPool() {
        simplePool = new FixedSimplePool<>(16);
    }

    public static BitmapPool getInstance() {
        return sInstance;
    }

    public Bitmap acquire(int width, int height) {
        Bitmap b = simplePool.acquire();
        if (null == b) {
            b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            if (b.getHeight() == height && b.getWidth() == width) {
                b.eraseColor(0);
            } else {
                b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        }
        return b;
    }

    public void release(Bitmap bitmap) {
        boolean isRelease = simplePool.release(bitmap);
        if (!isRelease) {
            bitmap.recycle();
        }
    }

    public synchronized void clear() {
        if (null == simplePool) {
            return;
        }
        Bitmap bitmap;
        while ((bitmap = simplePool.acquire()) != null) {
            bitmap.recycle();
        }
        //simplePool = null;
    }

    public static class FixedSimplePool<T> implements Pools.Pool<T> {
        private final Object[] mPool;

        private int mPoolSize;

        /**
         * Creates a new instance.
         *
         * @param maxPoolSize The max pool size.
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        public FixedSimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            mPool = new Object[maxPoolSize];
        }

        @Override
        @SuppressWarnings("unchecked")
        public T acquire() {
            if (mPoolSize > 0) {
                final int lastPooledIndex = mPoolSize - 1;
                T instance = (T) mPool[lastPooledIndex];
                mPool[lastPooledIndex] = null;
                mPoolSize--;
                return instance;
            }
            return null;
        }

        @Override
        public boolean release(@NonNull T instance) {
            if (isInPool(instance)) {
                return false;
            }
            if (mPoolSize < mPool.length) {
                mPool[mPoolSize] = instance;
                mPoolSize++;
                return true;
            }
            return false;
        }

        private boolean isInPool(@NonNull T instance) {
            for (int i = 0; i < mPoolSize; i++) {
                if (mPool[i] == instance) {
                    return true;
                }
            }
            return false;
        }
    }
}
