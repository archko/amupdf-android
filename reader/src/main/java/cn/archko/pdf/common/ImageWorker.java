package cn.archko.pdf.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Document;

import java.lang.ref.WeakReference;

import androidx.collection.LruCache;
import cn.archko.pdf.entity.APage;
import cn.archko.pdf.listeners.DecodeCallback;
import cn.archko.pdf.utils.Utils;

/**
 * @author: archko 2019/8/30 :16:17
 */
public abstract class ImageWorker {

    /**
     * Default transition drawable fade time
     */
    protected static final int FADE_IN_TIME = 200;

    /**
     * Default artwork
     */
    //protected BitmapDrawable mDefaultArtwork;

    /**
     * Default album art
     */
    //protected Bitmap mDefault;

    /**
     * The resources to use
     */
    protected Resources mResources;

    /**
     * First layer of the transition drawable
     */
    protected ColorDrawable mCurrentDrawable;

    /**
     * Layer drawable used to cross fade the result from the worker
     */
    protected Drawable[] mArrayDrawable;

    /**
     * The Context to use
     */
    protected Context mContext;

    /**
     * Disk and memory caches
     */

    private int mFadeInTime = FADE_IN_TIME;

    public void setFadeInTime(int fadeInTime) {
        if (fadeInTime > 100) {
            this.mFadeInTime = fadeInTime;
        }
    }

    /**
     * Constructor of <code>ImageWorker</code>
     *
     * @param context The {@link Context} to use
     */
    protected ImageWorker(final Context context) {
        mContext = context.getApplicationContext();
        mResources = mContext.getResources();
        // Create the transparent layer for the transition drawable
        mCurrentDrawable = new ColorDrawable(mResources.getColor(android.R.color.transparent));
        // A transparent image (layer 0) and the new result (layer 1)
        mArrayDrawable = new Drawable[2];
        mArrayDrawable[0] = mCurrentDrawable;
        // XXX The second layer is set in the worker task.
    }

    /**
     * @return True if the user is scrolling, false otherwise
     */
    public boolean isScrolling() {
        if (getImageCache() != null) {
            //return mImageCache.isScrolling();
        }
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

    public abstract LruCache<Object, Bitmap> getImageCache();

    public abstract LruCache<String, APage> getPageLruCache();

    /**
     * @return The deafult artwork
     */
    /*public Bitmap getDefaultArtwork() {
        return mDefault;
    }*/

    /**
     * The actual {@link AsyncTask} that will process the image.
     */
    protected final class BitmapWorkerTask extends AsyncTask<DecodeParam, Void, Bitmap> {

        /**
         * The {@link ImageView} used to set the result
         */
        private final WeakReference<ImageView> mImageReference;

        /**
         * The key used to store cached entries
         */
        DecodeParam decodeParam;

        /**
         * Constructor of <code>BitmapWorkerTask</code>
         *
         * @param imageView   The {@link ImageView} to use.
         * @param imageOption
         */
        @SuppressWarnings("deprecation")
        public BitmapWorkerTask(final ImageView imageView) {
            mImageReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * {@inheritDoc}
         */
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
            if (decodeParam.key != null && getImageCache() != null && !isCancelled()
                    && getAttachedImageView() != null) {
                bitmap = getBitmapFromCache(decodeParam.key);
            }

            // Define the album id now
            // Second, if we're fetching artwork, check the device for the image

            // Third, by now we need to download the image
            //Log.d("", "scheme:"+scheme+" url:"+url);
            if (bitmap == null && !isCancelled()
                    && getAttachedImageView() != null) {
                // Now define what the artist name, album name, and url are.
                //mUrl = processImageUrl();
                //bitmap = processBitmap(mUrl);
                bitmap = processBitmap(decodeParam);
            }

            // Fourth, add the new image to the cache
            if (bitmap != null && decodeParam.key != null && getImageCache() != null) {
                addBitmapToCache(decodeParam.key, bitmap);
            }

            // Add the second layer to the transiation drawable
            return bitmap;
        }

        /**
         * {@inheritDoc}
         */
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

    /**
     * Calls {@code cancel()} in the worker task
     *
     * @param imageView the {@link ImageView} to use
     */
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
            if (decodeParam == null || decodeParam.key == null || !decodeParam.key.equals(data)) {
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
        if (decodeParam.key == null || getImageCache() == null || decodeParam.imageView == null) {
            return;
        }
        // First, check the memory for the image
        final Bitmap lruBitmap = getBitmapFromCache(decodeParam.key);
        if (lruBitmap != null && decodeParam.imageView != null) {
            // Bitmap found in memory cache
            decodeParam.imageView.setImageBitmap(lruBitmap);
        } else if (executePotentialWork(decodeParam.key, decodeParam.imageView) && decodeParam.imageView != null && !isScrolling()) {
            // Otherwise run the worker task
            final BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(decodeParam.imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(bitmapWorkerTask);
            //imageView.setImageDrawable(asyncDrawable);
            decodeParam.imageView.setTag(asyncDrawable);
            // Don't execute the BitmapWorkerTask while scrolling
            if (isScrolling()) {
                cancelWork(decodeParam.imageView);
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

    public static class DecodeParam {
        public String key;
        public int pageNum;
        public float zoom;
        public int screenWidth;
        public ImageView imageView;
        public boolean crop;
        public int xOrigin;
        public APage pageSize;
        public Document document;
        public int targetWidth;
        DecodeCallback decodeCallback;

        public DecodeParam(String key, int pageNum, float zoom, int screenWidth, ImageView imageView) {
            this.key = key;
            this.pageNum = pageNum;
            this.zoom = zoom;
            this.screenWidth = screenWidth;
            this.imageView = imageView;
        }

        public DecodeParam(String key, ImageView imageView, boolean crop, int xOrigin,
                           APage pageSize, Document document, DecodeCallback callback) {
            this.key = key;
            if (TextUtils.isEmpty(key)) {
                this.key = String.format("%s,%s,%s,%s", imageView, crop, xOrigin, pageSize);
            }
            this.imageView = imageView;
            this.crop = crop;
            this.xOrigin = xOrigin;
            this.pageSize = pageSize;
            this.document = document;
            this.decodeCallback = callback;
        }
    }
}
