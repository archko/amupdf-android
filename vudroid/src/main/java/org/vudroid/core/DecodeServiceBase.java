package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;
import org.vudroid.pdfdroid.codec.PdfPage;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.archko.pdf.common.BitmapPool;

public class DecodeServiceBase implements DecodeService {
    private static final int PAGE_POOL_SIZE = 4;
    private static final int MSG_DECODE_START = 0;
    private static final int MSG_DECODE_FINISH = 4;
    private CodecContext codecContext;

    private View containerView;
    private CodecDocument document;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static final String TAG = "DecodeService";
    private final Map<Object, Future<?>> decodingFutures = new ConcurrentHashMap<>();
    private final SparseArray<SoftReference<CodecPage>> pages = new SparseArray<>();
    private Queue<Integer> pageEvictionQueue = new LinkedList<>();
    private int oriention = DocumentView.VERTICAL;
    private boolean isRecycled;
    Handler mHandler;
    private Handler.Callback mCallback = new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            if (what == MSG_DECODE_START) {
                final DecodeTask decodeTask = (DecodeTask) msg.obj;
                if (null != decodeTask) {
                    synchronized (decodingFutures) {
                        if (isRecycled) {
                            return true;
                        }
                        final Future<?> future = executorService.submit(() -> {
                            try {
                                Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
                                performDecode(decodeTask);
                            } catch (IOException e) {
                                Log.e(TAG, "Decode fail", e);
                            }
                        });
                        final Future<?> removed = decodingFutures.put(decodeTask.decodeKey, future);
                        if (removed != null) {
                            Log.e(TAG, "cancel Decode" + decodeTask);
                            removed.cancel(false);
                        }
                    }
                }
            } else if (what == MSG_DECODE_FINISH) {

            }
            return true;
        }
    };

    public DecodeServiceBase(CodecContext codecContext) {
        this.codecContext = codecContext;
        initDecodeThread();
    }

    public DecodeServiceBase() {
        codecContext = new PdfContext();
    }

    private void initDecodeThread() {
        HandlerThread handlerThread = new HandlerThread("decodeThread");
        handlerThread.start();
        // mHandler = new Handler(handlerThread.getLooper());
        mHandler = new Handler(handlerThread.getLooper(), mCallback);
    }

    public void setContainerView(View containerView) {
        this.containerView = containerView;
    }

    public void open(String filePath) {
        document = codecContext.openDocument(filePath);
    }

    public CodecDocument getDocument() {
        return document;
    }

    public void setDocument(CodecDocument document) {
        this.document = document;
        if (null != mHandler) {
            mHandler.sendEmptyMessage(MSG_DECODE_FINISH);
            mHandler.getLooper().quit();
        }
        initDecodeThread();
    }

    public void decodePage(Object decodeKey, PageTreeNode node, final DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds) {
        final DecodeTask decodeTask = new DecodeTask(node, decodeCallback, zoom, decodeKey, pageSliceBounds);
        Message message = Message.obtain();
        message.obj = decodeTask;
        message.what = MSG_DECODE_START;
        mHandler.sendMessage(message);
    }

    public void stopDecoding(Object decodeKey) {
        final Future<?> future = decodingFutures.remove(decodeKey);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void performDecode(DecodeTask task) throws IOException {
        if (isRecycled) {
            return;
        }
        if (isTaskDead(task)) {
            //Log.d(TAG, "Skipping decode task for page " + task);
            return;
        }
        //Log.d(TAG, "Starting decode of page: " + currentDecodeTask +" slice:"+currentDecodeTask.pageSliceBounds);
        CodecPage vuPage = getPage(task.pageNumber);
        preloadNextPage(task.pageNumber);

        if (isTaskDead(task)) {
            //Log.d(TAG, "Skipping decode when decoding task for page " + task);
            return;
        }
        //Log.d(TAG, "Start converting map to bitmap");
        float scale = calculateScale(vuPage) * task.zoom;
        //Log.d(TAG, "scale:"+scale+" vuPage.getWidth():"+vuPage.getWidth());
        Rect rect = getScaledSize(task, vuPage, scale);
        if (((PdfPage) vuPage).getPageHandle() < 0) {
            PdfDocument pdfDocument = (PdfDocument) document;
            ((PdfPage) vuPage).setPage(pdfDocument.getCore().loadPage(task.pageNumber));
        }

        if (task.node.page.links == null) {
            task.node.page.links = ((PdfPage) vuPage).getPageLinks();
        }
        final Bitmap bitmap = ((PdfPage) vuPage).renderBitmap(rect.width(), rect.height(), task.pageSliceBounds, scale);
        if (isTaskDead(task)) {
            Log.d(TAG, "decode bitmap dead:" + task);
            //bitmap.recycle();
            return;
        }

        finishDecoding(task, bitmap);
    }

    Rect getScaledSize(final DecodeTask task, final CodecPage vuPage, float scale) {
        Rect rect = new Rect();
        rect.right = getScaledWidth(task, vuPage, scale);
        rect.bottom = getScaledHeight(task, vuPage, scale);

        return rect;
    }

    private int getScaledHeight(DecodeTask task, CodecPage vuPage, float scale) {
        return Math.round(getScaledHeight(vuPage, scale) * task.pageSliceBounds.height());
    }

    private int getScaledWidth(DecodeTask task, CodecPage vuPage, float scale) {
        return Math.round(getScaledWidth(vuPage, scale) * task.pageSliceBounds.width());
    }

    private int getScaledHeight(CodecPage vuPage, float scale) {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(CodecPage vuPage, float scale) {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(CodecPage codecPage) {
        if (oriention == DocumentView.VERTICAL) {
            return 1.0f * getTargetWidth() / codecPage.getWidth();
        } else {
            return 1.0f * getTargetHeight() / codecPage.getHeight();
        }
    }

    private void finishDecoding(DecodeTask task, Bitmap bitmap) {
        updateImage(task, bitmap);
        //stopDecoding(currentDecodeTask.pageNumber);
        stopDecoding(task.decodeKey);
    }

    private void preloadNextPage(int pageNumber) {
        final int nextPage = pageNumber + 1;
        if (nextPage >= getPageCount()) {
            return;
        }
        getPage(nextPage);
    }

    public CodecPage getPage(int pageIndex) {
        if (null == pages.get(pageIndex) || pages.get(pageIndex).get() == null) {
            pages.put(pageIndex, new SoftReference<>(document.getPage(pageIndex)));
            pageEvictionQueue.remove(pageIndex);
            pageEvictionQueue.offer(pageIndex);
            if (pageEvictionQueue.size() > PAGE_POOL_SIZE) {
                Integer evictedPageIndex = pageEvictionQueue.poll();
                CodecPage evictedPage = pages.get(evictedPageIndex).get();
                pages.remove(evictedPageIndex);
                if (evictedPage != null) {
                    evictedPage.recycle();
                }
            }
        }
        return pages.get(pageIndex).get();
    }

    public Outline[] getOutlines() {
        return ((PdfDocument) document).getCore().loadOutline();
    }

    private void waitForDecode(CodecPage vuPage) {
        vuPage.waitForDecode();
    }

    public void setOriention(int oriention) {
        this.oriention = oriention;
    }

    private int getTargetWidth() {
        return containerView.getWidth();
    }

    private int getTargetHeight() {
        return containerView.getHeight();
    }

    public int getEffectivePagesWidth() {
        final CodecPage page = getPage(0);
        return getScaledWidth(page, calculateScale(page));
    }

    public int getEffectivePagesHeight() {
        final CodecPage page = getPage(0);
        return getScaledHeight(page, calculateScale(page));
    }

    public int getPageWidth(int pageIndex) {
        return getPage(pageIndex).getWidth();
    }

    public int getPageHeight(int pageIndex) {
        return getPage(pageIndex).getHeight();
    }

    private void updateImage(final DecodeTask task, Bitmap bitmap) {
        task.decodeCallback.decodeComplete(bitmap);
    }

    private boolean isTaskDead(DecodeTask task) {
        synchronized (decodingFutures) {
            return !decodingFutures.containsKey(task.decodeKey);
        }
    }

    public int getPageCount() {
        if (document == null) {
            return 0;
        }
        return document.getPageCount();
    }

    private class DecodeTask {
        private final Object decodeKey;
        final PageTreeNode node;
        private final int pageNumber;
        private final float zoom;
        private final DecodeCallback decodeCallback;
        private final RectF pageSliceBounds;

        private DecodeTask(PageTreeNode node, DecodeCallback decodeCallback, float zoom, Object decodeKey, RectF pageSliceBounds) {
            this.node = node;
            this.pageNumber = node.page.index;
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.decodeKey = decodeKey;
            this.pageSliceBounds = pageSliceBounds;
        }

        @Override
        public String toString() {
            return "DecodeTask{" +
                    "page=" + pageNumber +
                    ", zoom=" + zoom +
                    ", pageSliceBounds=" + pageSliceBounds +
                    ", decodeKey=" + decodeKey +
                    '}';
        }
    }

    public void recycle() {
        if (null != mHandler) {
            mHandler.sendEmptyMessage(MSG_DECODE_FINISH);
            mHandler.getLooper().quit();
        }
        synchronized (decodingFutures) {
            isRecycled = true;
        }
        for (Object key : decodingFutures.keySet()) {
            stopDecoding(key);
        }
        executorService.submit(() -> {
            //for (SoftReference<CodecPage> codecPageSoftReference : pages.values()) {
            int len = pages.size();
            SoftReference<CodecPage> codecPageSoftReference;
            for (int i = 0; i < len; i++) {
                codecPageSoftReference = pages.valueAt(i);
                CodecPage page = codecPageSoftReference.get();
                if (page != null) {
                    page.recycle();
                }
            }
            
            //这里不销毁document,由于在同一时期可能vm中有它的引用.在activity统一销毁
            //document.recycle();
            codecContext.recycle();

            BitmapPool.getInstance().clear();
        });
        executorService.shutdown();
        BitmapPool.getInstance().clear();
    }

    //=========================

}
