package cn.archko.pdf.core.cache;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

/**
 * Created by archko on 16/12/24.
 */

public class BitmapPool {

    private FixedSimplePool<Bitmap> simplePool;

    public static BitmapPool getInstance() {
        return BitmapPool.Factory.instance;
    }

    private static final class Factory {

        private static final BitmapPool instance = new BitmapPool();
    }

    private BitmapPool() {
        simplePool = new FixedSimplePool<>(18);
    }

    public Bitmap acquire(int width, int height) {
        Bitmap bitmap = simplePool.acquire();
        if (null != bitmap && bitmap.isRecycled()) {
            bitmap = simplePool.acquire();
        }
        if (null == bitmap) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            if (bitmap.getHeight() == height && bitmap.getWidth() == width) {
                //Log.d("TAG", String.format("use cache:%s-%s-%s%n", width, height, simplePool.mPoolSize));
                bitmap.eraseColor(0);
            } else {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        }
        return bitmap;
    }

    public Bitmap acquire(int width, int height, Bitmap.Config config) {
        Bitmap bitmap = simplePool.acquire();
        if (null != bitmap && bitmap.isRecycled()) {
            bitmap = simplePool.acquire();
        }
        if (null == bitmap) {
            bitmap = Bitmap.createBitmap(width, height, config);
        } else {
            if (bitmap.getConfig() == config) {
                if (bitmap.getHeight() == height && bitmap.getWidth() == width) {
                    //Log.d("TAG", String.format("use cache:%s-%s-%s%n", width, height, simplePool.mPoolSize));
                    bitmap.eraseColor(0);
                } else {
                    bitmap = Bitmap.createBitmap(width, height, config);
                }
            } else {
                bitmap = Bitmap.createBitmap(width, height, config);
            }
        }
        return bitmap;
    }

    public void release(Bitmap bitmap) {
        if (null == bitmap || bitmap.isRecycled()) {
            return;
        }
        boolean isRelease = simplePool.release(bitmap);
        if (!isRelease) {
            //System.out.println("recycle bitmap:" + bitmap);
            bitmap.recycle();
        }
    }

    public synchronized void clear() {
        if (null != simplePool) {
            Bitmap bitmap;
            while ((bitmap = simplePool.acquire()) != null) {
                bitmap.recycle();
            }
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
                return true;
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
