package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.PointF;
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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.entity.APage;

public class DecodeServiceBase implements DecodeService {
    private static final int PAGE_POOL_SIZE = 6;
    private static final int MSG_DECODE_START = 0;
    private static final int MSG_DECODE_SELECT = 1;
    private static final int MSG_DECODE_CANCEL = 2;
    private static final int MSG_DECODE_FINISH = 4;
    private CodecContext codecContext;

    private View containerView;
    private CodecDocument document;
    public static final String TAG = "DecodeService";
    private final LinkedHashMap<String, DecodeTask> nodeTasks = new LinkedHashMap<>(32, 0.75f, false);
    private final LinkedHashMap<String, DecodeTask> pageTasks = new LinkedHashMap<>(32, 0.75f, false);
    private final SparseArray<SoftReference<CodecPage>> pages = new SparseArray<>();
    private final Queue<Integer> pageEvictionQueue = new LinkedList<>();
    private int oriention = DocumentView.VERTICAL;
    private boolean isRecycled;
    Handler mHandler;
    private final List<APage> aPageList = new ArrayList<>();
    private final Handler.Callback mCallback = new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            if (what == MSG_DECODE_START) {
                addDecodeTask(msg);
            } else if (what == MSG_DECODE_SELECT) {
                selectDecodeTask(msg);
            } else if (what == MSG_DECODE_CANCEL) {
                cancelDecodeTask(msg);
            }
            return true;
        }

        private void addDecodeTask(Message msg) {
            final DecodeTask decodeTask = (DecodeTask) msg.obj;
            if (decodeTask.type == DecodeTask.TYPE_PAGE) {
                DecodeTask old = pageTasks.put(decodeTask.thumbKey, decodeTask);
                if (old != null) {
                    Log.d(TAG, String.format("old page task:%s-%s", pageTasks.size(), old));
                }
            } else {
                DecodeTask old = nodeTasks.put(decodeTask.decodeKey, decodeTask);
                if (old != null) {
                    Log.d(TAG, String.format("old node task:%s-%s", nodeTasks.size(), old));
                }
            }
            mHandler.sendEmptyMessage(MSG_DECODE_SELECT);
        }

        private void selectDecodeTask(Message msg) {
            if (isRecycled) {
                return;
            }

            DecodeTask selectTask = null;
            if (!pageTasks.isEmpty()) {
                selectTask = pageTasks.entrySet().iterator().next().getValue();
                pageTasks.remove(selectTask.thumbKey);
            }
            if (selectTask == null) {
                if (!nodeTasks.isEmpty()) {
                    selectTask = nodeTasks.entrySet().iterator().next().getValue();
                    nodeTasks.remove(selectTask.decodeKey);
                }
            }

            if (selectTask == null) {
                //mHandler.sendEmptyMessageDelayed(MSG_DECODE_SELECT, 5000L);
                Log.d(TAG, String.format("no task:%s-%s", pageTasks.size(), nodeTasks.size()));
            } else {
                Log.d(TAG, String.format("add task:%s-%s", selectTask.pageNumber, selectTask.type));
                try {
                    performDecode(selectTask);
                } catch (IOException e) {
                    Log.e(TAG, String.format("decode error:%s-%s", selectTask.pageNumber, selectTask.node));
                } finally {
                    mHandler.sendEmptyMessage(MSG_DECODE_SELECT);
                }
            }
        }

        private void cancelDecodeTask(Message msg) {
            String key = (String) msg.obj;
            DecodeTask remove = pageTasks.remove(key);
            if (remove != null) {
                remove.decodeCallback.decodeComplete(null, true);
            } else {
                remove = nodeTasks.remove(key);
                if (remove != null) {
                    remove.decodeCallback.decodeComplete(null, false);
                }
            }
        }
    };

    public DecodeServiceBase(CodecContext codecContext) {
        this.codecContext = codecContext;
        initDecodeThread();
    }

    private void initDecodeThread() {
        HandlerThread handlerThread = new HandlerThread("decodeThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), mCallback);
    }

    public void setContainerView(View containerView) {
        this.containerView = containerView;
    }

    public void open(String filePath) {
        long start = System.currentTimeMillis();
        document = codecContext.openDocument(filePath);
        int count = document.getPageCount();
        for (int i = 0; i < count; i++) {
            CodecPage codecPage = document.getPage(i);
            PointF pointF = new PointF(codecPage.getWidth(), codecPage.getHeight());
            APage aPage = new APage(i, pointF, 1f, 0);
            aPageList.add(aPage);
            codecPage.recycle();
        }
        Log.d(TAG, String.format("open.cos:%s", (System.currentTimeMillis() - start)));
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

    public void decodePage(String decodeKey, PageTreeNode node, int pageNumber, final DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds, String thumbKey) {
        final DecodeTask decodeTask = new DecodeTask(node, pageNumber, decodeCallback, zoom, decodeKey, pageSliceBounds, thumbKey);
        Message message = Message.obtain();
        message.obj = decodeTask;
        message.what = MSG_DECODE_START;
        mHandler.sendMessage(message);
    }

    public void stopDecoding(String decodeKey) {
        pageTasks.remove(decodeKey);
        Message message = Message.obtain();
        message.obj = decodeKey;
        message.what = MSG_DECODE_CANCEL;
        mHandler.sendMessage(message);
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

        if (vuPage.isRecycle()) {
            vuPage.loadPage(task.pageNumber);
        }

        if (task.type == DecodeTask.TYPE_PAGE) {
            decodeThumb(task, vuPage);
            return;
        }

        Bitmap bitmap = BitmapCache.getInstance().getNodeBitmap(task.decodeKey);
        if (null != bitmap) {
            finishDecoding(task, bitmap);
            return;
        }
        if (isTaskDead(task)) {
            //Log.d(TAG, "Skipping decode when decoding task for page " + task);
            return;
        }

        //Log.d(TAG, "Start converting map to bitmap");
        float scale = calculateScale(vuPage) * task.zoom;
        //Log.d(TAG, "scale:"+scale+" vuPage.getWidth():"+vuPage.getWidth());
        Rect rect = getScaledSize(task, vuPage, scale);

        if (null != task.node && task.node.page.links == null) {
            task.node.page.links = vuPage.getPageLinks();
        }

        Log.d(TAG, String.format("renderBitmap:%s, w-h:%s", task.pageNumber, rect));
        bitmap = vuPage.renderBitmap(rect.width(), rect.height(), task.pageSliceBounds, scale);
        if (null != bitmap) {
            BitmapCache.getInstance().addNodeBitmap(task.decodeKey, bitmap);
        }
        if (isTaskDead(task)) {
            //Log.d(TAG, "decode bitmap dead:" + task);
            BitmapPool.getInstance().release(bitmap);
            return;
        }

        finishDecoding(task, bitmap);
    }

    private void decodeThumb(DecodeTask task, CodecPage vuPage) {
        Bitmap thumb = BitmapCache.getInstance().getBitmap(task.thumbKey);
        if (null != thumb) {
            updateThumb(task, thumb);
        } else {
            float xs = 1f;
            if (oriention == DocumentView.VERTICAL) {
                xs = 1.0f * getTargetWidth() / vuPage.getWidth() / 4;
            } else {
                xs = 1.0f * getTargetHeight() / vuPage.getHeight() / 4;
            }
            int width = (int) (xs * vuPage.getWidth());
            int height = (int) (xs * vuPage.getHeight());
            Log.d(TAG, String.format("decodeThumb:%s, w-h:%s-%s-%s, %s", task.pageNumber, width, height, xs, task.thumbKey));
            thumb = vuPage.renderBitmap(
                    width,
                    height,
                    new RectF(0, 0, 1, 1),
                    xs);
            //PDFUtils.saveBitmapToFile(thumb, new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/book/" + task.pageNumber + "-" + System.currentTimeMillis() + ".png"));
            if (null != thumb) {
                BitmapCache.getInstance().addBitmap(task.thumbKey, thumb);
            }
            //if (isTaskDead(task)) {
            //    return;
            //}
            updateThumb(task, thumb);
        }
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

    private int getScaledHeight(APage vuPage, float scale) {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(CodecPage vuPage, float scale) {
        return (int) (scale * vuPage.getWidth());
    }

    private int getScaledWidth(APage vuPage, float scale) {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(CodecPage codecPage) {
        if (oriention == DocumentView.VERTICAL) {
            return 1.0f * getTargetWidth() / codecPage.getWidth();
        } else {
            return 1.0f * getTargetHeight() / codecPage.getHeight();
        }
    }

    private float calculateScale(APage codecPage) {
        if (oriention == DocumentView.VERTICAL) {
            return 1.0f * getTargetWidth() / codecPage.getWidth();
        } else {
            return 1.0f * getTargetHeight() / codecPage.getHeight();
        }
    }

    private void finishDecoding(DecodeTask task, Bitmap bitmap) {
        updateImage(task, bitmap);
        //stopDecoding(task.decodeKey);
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
        return document.loadOutline();
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

    public int getEffectivePagesWidth(int index) {
        //final CodecPage page = getPage();
        final APage page = aPageList.get(index);
        return getScaledWidth(page, calculateScale(page));
    }

    public int getEffectivePagesHeight(int index) {
        //final CodecPage page = getPage(0);
        final APage page = aPageList.get(index);
        return getScaledHeight(page, calculateScale(page));
    }

    public int getPageWidth(int pageIndex) {
        return getPage(pageIndex).getWidth();
    }

    public int getPageHeight(int pageIndex) {
        return getPage(pageIndex).getHeight();
    }

    private void updateImage(final DecodeTask task, Bitmap bitmap) {
        task.decodeCallback.decodeComplete(bitmap, false);
    }

    private void updateThumb(final DecodeTask task, Bitmap bitmap) {
        task.decodeCallback.decodeComplete(bitmap, true);
        //stopDecoding(task.decodeKey);
    }

    private boolean skipInvisible(DecodeTask task, boolean isFullPage) {
        if (!task.decodeCallback.shouldRender(task.pageNumber, isFullPage)) {
            //Log.d(TAG, String.format("should not Render:%s- waiting:%s", task.pageNumber, decodingFutures.size()));
            stopDecoding(task.decodeKey);
            return true;
        }
        return false;
    }

    private boolean isTaskDead(DecodeTask task) {
        boolean isPage = false;
        if (task.node == null) {
            isPage = true;
        }
        if (skipInvisible(task, isPage)) {
            return true;
        }
        return false;
    }

    public int getPageCount() {
        if (document == null) {
            return 0;
        }
        return document.getPageCount();
    }

    private class DecodeTask {
        private static final int TYPE_PAGE = 0;
        private static final int TYPE_NODE = 1;
        private final String decodeKey;
        final PageTreeNode node;
        private final int pageNumber;
        private final int type;
        private final float zoom;
        private final DecodeCallback decodeCallback;
        private final RectF pageSliceBounds;
        private String thumbKey;

        private DecodeTask(PageTreeNode node, int pageNumber, DecodeCallback decodeCallback, float zoom, String decodeKey, RectF pageSliceBounds, String thumbKey) {
            this.node = node;
            this.type = node == null ? TYPE_PAGE : TYPE_NODE;
            this.pageNumber = pageNumber;
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
            this.decodeKey = decodeKey;
            this.pageSliceBounds = pageSliceBounds;
            this.thumbKey = thumbKey;
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
        synchronized (nodeTasks) {
            isRecycled = true;
        }
        for (String key : nodeTasks.keySet()) {
            stopDecoding(key);
        }
        new Thread(() -> {
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
            document.recycle();
            codecContext.recycle();
            BitmapPool.getInstance().clear();
        }).start();
    }

    //=========================

}
