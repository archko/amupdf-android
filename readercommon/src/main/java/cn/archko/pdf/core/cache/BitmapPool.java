package cn.archko.pdf.core.cache;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.util.Pools;

/**
 * Created by archko on 16/12/24.
 */
public class BitmapPool {

    private final FixedSimplePool<Bitmap> simplePool;
    private final Object mLock = new Object();

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
        return acquire(width, height, Bitmap.Config.ARGB_8888);
    }

    public Bitmap acquire(int width, int height, Bitmap.Config config) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        final Bitmap.Config finalConfig = (config == null) ? Bitmap.Config.ARGB_8888 : config;
        Bitmap bitmap = null;

        synchronized (mLock) {
            do {
                bitmap = simplePool.acquire();
            } while (bitmap != null && (bitmap.isRecycled() || bitmap.getConfig() != finalConfig));
        }

        if (bitmap != null) {
            if (bitmap.getWidth() == width && bitmap.getHeight() == height) {
                try {
                    // 擦除颜色（0表示透明），复用前清空画布
                    bitmap.eraseColor(0);
                } catch (Exception e) {
                    // 极端情况擦除失败，放弃复用新建
                    bitmap = createBitmapSafe(width, height, finalConfig);
                }
            } else {
                // 宽高不匹配，新建
                bitmap = createBitmapSafe(width, height, finalConfig);
            }
        } else {
            // 池内无可用Bitmap，新建
            bitmap = createBitmapSafe(width, height, finalConfig);
        }

        return bitmap;
    }

    public void release(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || !bitmap.isMutable()) {
            return;
        }

        synchronized (mLock) {
            boolean isRelease = simplePool.release(bitmap);
            if (!isRelease) {
                //System.out.println("recycle bitmap:" + bitmap);
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }

    public void clear() {
        synchronized (mLock) {
            if (simplePool == null) {
                return;
            }
            Bitmap bitmap;
            while ((bitmap = simplePool.acquire()) != null) {
                try {
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                } catch (Exception e) {
                    // 忽略回收异常，保证清空流程完成
                }
            }
        }
    }

    private Bitmap createBitmapSafe(int width, int height, @NonNull Bitmap.Config config) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError | IllegalArgumentException e) {
            // 内存不足/参数错误时，尝试降级为RGB_565（更省内存）
            try {
                return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            } catch (Exception ex) {
                // 最终兜底，抛出明确异常
                throw new RuntimeException("create bitmap failed, width:" + width + ", height:" + height, ex);
            }
        }
    }

    public static class FixedSimplePool<T> implements Pools.Pool<T> {
        private final Object[] mPool;
        private int mPoolSize;

        public FixedSimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("The max pool size must be > 0");
            }
            mPool = new Object[maxPoolSize];
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized T acquire() {
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
        public synchronized boolean release(@NonNull T instance) {
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

        private synchronized boolean isInPool(@NonNull T instance) {
            for (int i = 0; i < mPoolSize; i++) {
                if (mPool[i] == instance) {
                    return true;
                }
            }
            return false;
        }
    }
}
