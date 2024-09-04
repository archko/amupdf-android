package cn.archko.pdf.decode;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.github.axet.k2pdfopt.K2PdfOpt;

import org.vudroid.core.DecodeService;
import org.vudroid.core.DecodeTask;
import org.vudroid.core.DocumentView;
import org.vudroid.core.PageTreeNode;
import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import cn.archko.pdf.common.ReflowHelper;
import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.cache.BitmapPool;
import cn.archko.pdf.core.common.APageSizeLoader;
import cn.archko.pdf.core.entity.APage;
import cn.archko.pdf.core.utils.CropUtils;

public class DocDecodeService {

    public interface IView {
        int getWidth();

        int getHeight();
    }

    private static final int PAGE_POOL_SIZE = 6;
    private static final int MSG_DECODE_START = 0;
    private static final int MSG_DECODE_SELECT = 1;
    private static final int MSG_DECODE_CANCEL = 2;
    private static final int MSG_DECODE_FINISH = 4;
    private static final int MSG_DECODE_TASK = 5;
    private CodecContext codecContext;

    private IView containerView;
    private CodecDocument document;
    public static final String TAG = "DecodeService";
    private final HashMap<String, DecodeTask> nodeTasks = new HashMap<>(64, 0.75f);
    private final ConcurrentHashMap<String, DecodeTask> decodingTasks = new ConcurrentHashMap<>(2);
    private final SparseArray<SoftReference<CodecPage>> pages = new SparseArray<>();
    private final Queue<Integer> pageEvictionQueue = new LinkedList<>();
    private int oriention = DocumentView.VERTICAL;
    private boolean isRecycled;
    private Handler mHandler;
    private Handler mDecodeHandler;
    private final List<APage> aPageList = new ArrayList<>();
    private APageSizeLoader.PageSizeBean pageSizeBean;

    private K2PdfOpt k2PdfOpt = new K2PdfOpt();
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
            DecodeTask old = nodeTasks.put(decodeTask.decodeKey, decodeTask);
            if (old != null) {
                Log.d(TAG, String.format("old node task:%s-%s", nodeTasks.size(), old));
            }
            if (nodeTasks.size() <= 1) {
                mHandler.sendEmptyMessage(MSG_DECODE_SELECT);
            }
        }

        private void selectDecodeTask(Message msg) {
            if (isRecycled) {
                return;
            }

            if (!decodingTasks.isEmpty()) {
                Log.d(TAG, String.format("running task>1:%s-%s", decodingTasks.size(), nodeTasks.size()));
                return;
            }

            DecodeTask selectTask = null;
            if (!nodeTasks.isEmpty()) {
                selectTask = nodeTasks.entrySet().iterator().next().getValue();
                nodeTasks.remove(selectTask.decodeKey);
            }

            if (selectTask == null) {
                Log.d(TAG, String.format("no task:%s", nodeTasks.size()));
            } else {
                decodingTasks.put(selectTask.decodeKey, selectTask);
                mDecodeHandler.sendEmptyMessage(MSG_DECODE_TASK);
            }
        }

        private void cancelDecodeTask(Message msg) {
            String key = (String) msg.obj;
            DecodeTask remove = nodeTasks.remove(key);
            if (remove != null) {
                remove.decodeCallback.decodeComplete(null, false, null);
            }
        }
    };
    private final Handler.Callback mDecodeCallback = new Handler.Callback() {
        public boolean handleMessage(Message msg) {
            DecodeTask selectTask = null;
            synchronized (decodingTasks) {
                if (!decodingTasks.isEmpty()) {
                    selectTask = decodingTasks.entrySet().iterator().next().getValue();
                }
            }
            if (selectTask == null) {
                mHandler.sendEmptyMessage(MSG_DECODE_SELECT);
            } else {
                try {
                    Log.d(TAG, String.format("add task:%s-%s, %s-%s", selectTask.pageNumber, selectTask.crop, selectTask.decodeKey, selectTask.pageSliceBounds));
                    performDecode(selectTask);
                } catch (IOException e) {
                    Log.e(TAG, String.format("decode error:%s", selectTask.pageNumber));
                } finally {
                    decodingTasks.remove(selectTask.decodeKey);
                    mHandler.sendEmptyMessage(MSG_DECODE_SELECT);
                }
            }

            return true;
        }
    };

    public DocDecodeService(CodecContext codecContext) {
        this.codecContext = codecContext;
        initDecodeThread();
    }

    private void initDecodeThread() {
        HandlerThread handlerThread = new HandlerThread("taskThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), mCallback);
        handlerThread = new HandlerThread("decodeThread");
        handlerThread.start();
        mDecodeHandler = new Handler(handlerThread.getLooper(), mDecodeCallback);
    }

    public void setContainerView(IView containerView) {
        this.containerView = containerView;
    }

    public CodecDocument open(String path, boolean crop, boolean cachePage) {
        aPageList.clear();
        long start = System.currentTimeMillis();
        document = codecContext.openDocument(path);
        if (null == document) {
            return null;
        }
        int count = document.getPageCount();
        APageSizeLoader.PageSizeBean psb = APageSizeLoader.INSTANCE.loadPageSizeFromFile(count, path);
        if (null != psb) {
            pageSizeBean = psb;
            aPageList.addAll(psb.getList());
            return document;
        } else {
            pageSizeBean = new APageSizeLoader.PageSizeBean();
            pageSizeBean.setList(aPageList);
        }
        try {
            for (int i = 0; i < count; i++) {
                CodecPage codecPage = document.getPage(i);
                APage aPage = new APage(i, codecPage.getWidth(), codecPage.getHeight(), 1f);
                aPageList.add(aPage);
                codecPage.recycle();
            }

            if (cachePage) {
                APageSizeLoader.INSTANCE.savePageSizeToFile(crop, path, aPageList);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, String.format("open.cos:%s", (System.currentTimeMillis() - start)));
        return document;
    }

    public APageSizeLoader.PageSizeBean getPageSizeBean() {
        return pageSizeBean;
    }

    private Rect cropPage(CodecPage vuPage) {
        int width = 300;
        float ratio = 1f * vuPage.getWidth() / width;
        int height = (int) (vuPage.getHeight() / ratio);
        Bitmap thumb = vuPage.renderBitmap(
                new Rect(0, 0, width, height),
                width,
                height,
                new RectF(0, 0, 1, 1),
                1 / ratio);

        RectF cropBounds = CropUtils.getJavaCropBounds(
                thumb,
                new Rect(0, 0, thumb.getWidth(), thumb.getHeight())
        );
        BitmapPool.getInstance().release(thumb);

        int leftBound = (int) (cropBounds.left * ratio);
        int topBound = (int) (cropBounds.top * ratio);
        int resultW = (int) (cropBounds.width() * ratio);
        int resultH = (int) (cropBounds.height() * ratio);
        Rect rect = new Rect(leftBound, topBound, leftBound + resultW, topBound + resultH);
        //Log.d("TAG", String.format("cropPage:%s, w-h:%s-%s, %s, %s",
        //        ratio, width, height, cropBounds, rect));
        return rect;
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

    public void decodePage(String decodeKey, PageTreeNode node, boolean crop, int pageNumber, final DecodeService.DecodeCallback decodeCallback, float zoom, RectF pageSliceBounds, int dpi) {
        final DecodeTask decodeTask = new DecodeTask(node, crop, pageNumber, decodeCallback, zoom, decodeKey, pageSliceBounds, dpi);
        Message message = Message.obtain();
        message.obj = decodeTask;
        message.what = MSG_DECODE_START;
        mHandler.sendMessage(message);
    }

    public void stopDecoding(String decodeKey) {
        if (isRecycled) {
            return;
        }
        Message message = Message.obtain();
        message.obj = decodeKey;
        message.what = MSG_DECODE_CANCEL;
        mHandler.sendMessage(message);
    }

    private void performDecode(DecodeTask task) throws IOException {
        if (isRecycled || isTaskDead(task)) {
            return;
        }
        CodecPage vuPage = getPage(task.pageNumber);
        preloadNextPage(task.pageNumber);

        if (vuPage.isRecycle()) {
            vuPage.loadPage(task.pageNumber);
        }

        Bitmap bitmap = BitmapCache.getInstance().getBitmap(task.decodeKey);
        if (null != bitmap) {
            Log.d("TAG", String.format("performDecode.hit cache:%s-%s", task.pageNumber, task.decodeKey));
            k2pdfAndFinish(task, bitmap);
            return;
        }
        if (isTaskDead(task)) {
            return;
        }

        APage aPage = aPageList.get(task.pageNumber);

        Rect cropBounds;
        if (task.crop) {
            cropBounds = cropPage(vuPage);
            aPage.setCropBounds(cropBounds);
        } else {
            cropBounds = new Rect(0, 0, (int) aPage.getWidth(), (int) aPage.getHeight());
        }

        //scale需要在切边后计算,否则缩放值是不对
        float scale = calculateScale(aPage, task.crop) * task.zoom;
        Rect rect = getScaledSize(task, aPage, scale, task.crop);
        bitmap = vuPage.renderBitmap(
                cropBounds,
                rect.width(),
                rect.height(),
                task.pageSliceBounds,
                scale);
        if (null != bitmap) {
            BitmapCache.getInstance().addBitmap(task.decodeKey, bitmap);
        }
        if (isTaskDead(task)) {
            //Log.d(TAG, "decode bitmap dead:" + task);
            BitmapPool.getInstance().release(bitmap);
            return;
        }

        k2pdfAndFinish(task, bitmap);
    }

    private void k2pdfAndFinish(DecodeTask task, Bitmap bitmap) {
        List<Bitmap> bitmaps = null;
        if (task.dpi > 0) {
            bitmaps = ReflowHelper.INSTANCE.k2pdf2bitmap(
                    k2PdfOpt,
                    1f,
                    bitmap,
                    getTargetWidth(),
                    getTargetHeight(),
                    task.dpi
            );
        }

        finishDecoding(task, bitmap, bitmaps);
    }

    Rect getScaledSize(final DecodeTask task, final APage vuPage, float scale, boolean crop) {
        Rect rect = new Rect();
        rect.right = getScaledWidth(task, vuPage, scale, crop);
        rect.bottom = getScaledHeight(task, vuPage, scale, crop);

        return rect;
    }

    private int getScaledHeight(DecodeTask task, APage vuPage, float scale, boolean crop) {
        return Math.round(getScaledHeight(vuPage, scale, crop) * task.pageSliceBounds.height());
    }

    private int getScaledWidth(DecodeTask task, APage vuPage, float scale, boolean crop) {
        return Math.round(getScaledWidth(vuPage, scale, crop) * task.pageSliceBounds.width());
    }

    private int getScaledHeight(APage vuPage, float scale, boolean crop) {
        return (int) (scale * vuPage.getHeight(crop));
    }

    private int getScaledWidth(APage vuPage, float scale, boolean crop) {
        return (int) (scale * vuPage.getWidth(crop));
    }

    private float calculateScale(APage codecPage, boolean crop) {
        if (oriention == DocumentView.VERTICAL) {
            return 1.0f * getTargetWidth() / codecPage.getWidth(crop);
        } else {
            return 1.0f * getTargetHeight() / codecPage.getHeight(crop);
        }
    }

    private void finishDecoding(DecodeTask task, Bitmap bitmap, Object args) {
        updateImage(task, bitmap, args);
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

    public APage getAPage(int pageIndex) {
        return aPageList.get(pageIndex);
    }

    private int getTargetWidth() {
        return containerView.getWidth();
    }

    private int getTargetHeight() {
        return containerView.getHeight();
    }

    private void updateImage(final DecodeTask task, Bitmap bitmap, Object args) {
        task.decodeCallback.decodeComplete(bitmap, false, args);
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
        boolean isPage = true;
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

    public void recycle() {
        if (null != mHandler) {
            mHandler.sendEmptyMessage(MSG_DECODE_FINISH);
            mHandler.getLooper().quit();
        }
        if (null != mDecodeHandler) {
            mDecodeHandler.getLooper().quit();
        }
        synchronized (nodeTasks) {
            isRecycled = true;
        }
        for (String key : nodeTasks.keySet()) {
            stopDecoding(key);
        }
        new Thread(() -> {
            int len = pages.size();
            SoftReference<CodecPage> codecPageSoftReference;
            for (int i = 0; i < len; i++) {
                codecPageSoftReference = pages.valueAt(i);
                CodecPage page = codecPageSoftReference.get();
                if (page != null) {
                    page.recycle();
                }
            }
            if (null != document) {
                document.recycle();
            }
            if (null != codecContext) {
                codecContext.recycle();
            }
            BitmapPool.getInstance().clear();
        }).start();
    }

    //=========================

}
