package cn.archko.pdf.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import androidx.collection.LruCache;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.entity.DecodeParam;
import cn.archko.pdf.utils.Utils;

/**
 * @author: archko 2019/8/30 :16:17
 */
public abstract class ImageWorker {

    protected ImageWorker(final Context context) {
    }

    public boolean isScrolling() {
        //if (getImageCache() != null) {
        //    //return mImageCache.isScrolling();
        //}
        return false;
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param data   The key used to store the image
     * @param bitmap The {@link Bitmap} to cache
     */
    public abstract void addBitmapToCache(final String key, final Bitmap bitmap);

    public abstract Bitmap getBitmapFromCache(final String key);

    public abstract LruCache<String, APage> getPageLruCache();

    protected final class BitmapWorkerTask extends AsyncTask<DecodeParam, Void, Bitmap> {

        private final WeakReference<ImageView> mImageReference;

        DecodeParam decodeParam;

        public BitmapWorkerTask(final ImageView imageView) {
            mImageReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(final DecodeParam... params) {
            // Define the key
            decodeParam = params[0];

            // The result
            Bitmap bitmap = null;

            // Wait here if work is paused and the task is not cancelled. This
            // shouldn't even occur because this isn't executing while the user
            // is scrolling, but just in case.
            while (isScrolling() && !isCancelled()) {
                cancel(true);
            }

            // First, check the disk cache for the image
            if (decodeParam.getKey() != null /*&& getImageCache() != null*/ && !isCancelled()
                    && getAttachedImageView() != null) {
                bitmap = getBitmapFromCache(decodeParam.getKey());
            }

            if (bitmap == null && !isCancelled()
                    && getAttachedImageView() != null) {
                bitmap = processBitmap(decodeParam);
            }

            // Fourth, add the new image to the cache
            if (bitmap != null && decodeParam.getKey() != null /*&& getImageCache() != null*/) {
                addBitmapToCache(decodeParam.getKey(), bitmap);
            }

            // Add the second layer to the transiation drawable
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            postBitmap(this, bitmap, this.decodeParam);
        }

        /**
         * @return The {@link ImageView} associated with this task as long as
         * the ImageView's task still points to this task as well.
         * Returns null otherwise.
         */
        protected final ImageView getAttachedImageView() {
            final ImageView imageView = mImageReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask) {
                return imageView;
            }
            return null;
        }
    }

    protected void postBitmap(BitmapWorkerTask bitmapWorkerTask, Bitmap result, DecodeParam decodeParam) {
        //if (bitmapWorkerTask.isCancelled()) {
        //    result = null;
        //}
        //final ImageView imageView = bitmapWorkerTask.getAttachedImageView();
        //if (result != null && imageView != null) {  //TODO if load image failed...
        //    if (null == imageView.getDrawable() || imageView.getDrawable() instanceof BitmapDrawable) {
        //        imageView.setImageDrawable(result);
        //    } else {
        //        Bitmap bitmap = getBitmapFromCache(decodeParam.key);
        //        if (null != bitmap) {
        //            imageView.setImageBitmap(bitmap);
        //        }
        //    }
        //}
    }

    public static final void cancelWork(final ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no
     * work in progress on this image view. Returns false if the work in
     * progress deals with the same data. The work is not stopped in that case.
     */
    public static final boolean executePotentialWork(final Object data, final ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            final DecodeParam decodeParam = bitmapWorkerTask.decodeParam;
            if (decodeParam == null || decodeParam.getKey() == null || !decodeParam.getKey().equals(data)) {
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        return true;
    }

    /**
     * Used to determine if the current image drawable has an instance of
     * {@link BitmapWorkerTask}
     *
     * @param imageView Any {@link ImageView}.
     * @return Retrieve the currently active work task (if any) associated with
     * this {@link ImageView}. null if there is no such task.
     */
    private static final BitmapWorkerTask getBitmapWorkerTask(final ImageView imageView) {
        if (imageView != null) {
            final AsyncDrawable tag = (AsyncDrawable) imageView.getTag();
            if (null != tag) {
                return tag.getBitmapWorkerTask();
            }
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * A custom {@link BitmapDrawable} that will be attached to the
     * {@link ImageView} while the work is in progress. Contains a reference to
     * the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can
     * bind its result, independently of the finish order.
     */
    private static final class AsyncDrawable extends ColorDrawable {

        private final WeakReference<BitmapWorkerTask> mBitmapWorkerTaskReference;

        /**
         * Constructor of <code>AsyncDrawable</code>
         */
        public AsyncDrawable(final BitmapWorkerTask mBitmapWorkerTask) {
            super(Color.TRANSPARENT);
            mBitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(mBitmapWorkerTask);
        }

        /**
         * @return The {@link BitmapWorkerTask} associated with this drawable
         */
        public BitmapWorkerTask getBitmapWorkerTask() {
            return mBitmapWorkerTaskReference.get();
        }
    }

    public void loadImage(DecodeParam decodeParam) {
        loadImage(decodeParam, true);
    }

    public void loadImage(DecodeParam decodeParam, boolean forceSerial) {
        if (decodeParam.getKey() == null /*|| decodeParam.imageView == null*/) {
            return;
        }
        final Bitmap lruBitmap = getBitmapFromCache(decodeParam.getKey());
        if (lruBitmap != null /*&& decodeParam.imageView != null*/) {
            decodeParam.getImageView().setImageBitmap(lruBitmap);
        } else if (executePotentialWork(decodeParam.getKey(), decodeParam.getImageView()) && decodeParam.getImageView() != null && !isScrolling()) {
            // Otherwise run the worker task
            final BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(decodeParam.getImageView());
            final AsyncDrawable asyncDrawable = new AsyncDrawable(bitmapWorkerTask);
            decodeParam.getImageView().setTag(asyncDrawable);
            // Don't execute the BitmapWorkerTask while scrolling
            if (isScrolling()) {
                cancelWork(decodeParam.getImageView());
            } else {
                Utils.execute(forceSerial, bitmapWorkerTask, decodeParam);
            }
        }
    }

    /**
     * Subclasses should override this to define any processing or work that
     * must happen to produce the final {@link Bitmap}. This will be executed in
     * a background thread and be long running.
     *
     * @param key          The key to identify which image to process, as provided by
     *                     {@link com.andrew.apollo.cache.loadImage(mKey, ImageView)}
     * @param mImageOption
     * @return The processed {@link Bitmap}.
     */
    protected abstract Bitmap processBitmap(DecodeParam decodeParam);

    public void recycle() {
    }

}
