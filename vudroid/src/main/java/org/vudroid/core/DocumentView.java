package org.vudroid.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.events.ZoomListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.multitouch.MultiTouchZoom;

import cn.archko.pdf.listeners.SimpleGestureListener;
import cn.archko.pdf.widgets.Flinger;

public class DocumentView extends View implements ZoomListener {

    public static final String TAG = "DocumentView";
    final ZoomModel zoomModel;
    private final CurrentPageModel currentPageModel;
    DecodeService decodeService;
    private final SparseArray<Page> pages = new SparseArray<>();
    private boolean isInitialized = false;
    private int pageToGoTo;
    private int xToScroll;
    private int yToScroll;
    private final Flinger scroller;
    DecodingProgressModel progressModel;
    private RectF viewRect;
    private boolean inZoom;
    private long lastDownEventTime;
    private static final int DOUBLE_TAP_TIME = 600;
    private MultiTouchZoom multiTouchZoom;
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;
    private int oriention = VERTICAL;

    private final GestureDetector mGestureDetector;
    int mMargin = 16;
    int preDecodePage = 1;
    private SimpleGestureListener simpleGestureListener;

    public int getOriention() {
        return oriention;
    }

    public void setOriention(int oriention) {
        if (this.oriention != oriention) {
            this.oriention = oriention;
            pageToGoTo = getCurrentPage();
            requestLayout();
            if (null != decodeService) {
                decodeService.setOriention(oriention);
            }
        }
    }
    /*public void setPageModel(CurrentPageModel mPageModel) {
        this.mPageModel = mPageModel;
    }*/

    public DocumentView(Context context, final ZoomModel zoomModel, DecodingProgressModel progressModel, CurrentPageModel currentPageModel, SimpleGestureListener simpleGestureListener) {
        super(context);
        this.zoomModel = zoomModel;
        this.progressModel = progressModel;
        this.currentPageModel = currentPageModel;
        setKeepScreenOn(true);
        scroller = new Flinger();
        setFocusable(true);
        setFocusableInTouchMode(true);
        initMultiTouchZoomIfAvailable(zoomModel);
        mGestureDetector = new GestureDetector(context, new MySimpleOnGestureListener());
        this.simpleGestureListener = simpleGestureListener;
    }

    private void initMultiTouchZoomIfAvailable(ZoomModel zoomModel) {
        try {
            multiTouchZoom = (MultiTouchZoom) Class.forName("org.vudroid.core.multitouch.MultiTouchZoomImpl").getConstructor(ZoomModel.class).newInstance(zoomModel);
        } catch (Exception e) {
            Log.d(TAG, "Multi touch zoom is not available: " + e);
        }
    }

    public void setDecodeService(DecodeService decodeService) {
        this.decodeService = decodeService;
        decodeService.setOriention(oriention);
    }

    private void init() {
        if (isInitialized || decodeService.getPageCount() < 1) {
            return;
        }
        for (int i = 0; i < decodeService.getPageCount(); i++) {
            final int width = decodeService.getEffectivePagesWidth(i);
            final int height = decodeService.getEffectivePagesHeight(i);
            pages.put(i, new Page(this, i));
            pages.get(i).setAspectRatio(width, height);
        }
        Log.d(TAG, "DecodeService:" + pages.size() + " pageToGoTo:" + pageToGoTo);
        isInitialized = true;
        currentPageModel.setPageCount(decodeService.getPageCount());
        invalidatePageSizes();
        goToPageImpl(pageToGoTo);
    }

    private void goToPageImpl(final int toPage) {
        Page page = pages.get(toPage);  //TODO ,page is not really page on the first time.
        if (null == page) {
            Log.d(TAG, String.format("goToPageImpl.error:%s-%s", toPage, pages.size()));
            return;
        }
        int scrollX = 0;
        int scrollY = 0;
        if (oriention == VERTICAL) {
            scrollX = getScrollX();
            scrollY = page.getTop();
            if (xToScroll != 0) {
                scrollX = xToScroll;
                xToScroll = 0;
            }
            if (yToScroll != 0) {
                if (page.getBottom() > yToScroll) {
                    scrollY = yToScroll;
                }
                yToScroll = 0;
            }
        } else {
            scrollX = page.getLeft();
            scrollY = getScrollY();
            if (yToScroll != 0) {
                scrollY = yToScroll;
                yToScroll = 0;
            }
            if (xToScroll != 0) {
                if (page.getRight() > xToScroll) {
                    scrollX = xToScroll;
                }
                xToScroll = 0;
            }
        }
        Log.d(VIEW_LOG_TAG, "goToPageImpl:" + xToScroll + " scroll:" + scrollX + " yToScroll:" + yToScroll + " scrollY:" + scrollY + " page:" + page);

        scrollTo(scrollX, scrollY);
        pageToGoTo = -1;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // bounds could be not updated
        currentPageChanged();
        if (inZoom) {
            return;
        }
        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        post(() -> updatePageVisibility());
    }

    private void currentPageChanged() {
        post(() -> {
            //currentPageModel.setCurrentPageIndex(getCurrentPage());
            currentPageModel.setCurrentPage(getCurrentPage());
        });
    }

    private void updatePageVisibility() {
        //for (Page page : pages.values()) {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.updateVisibility();
        }
    }

    public void commitZoom() {
        //for (Page page : pages.values()) {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.invalidate();
        }
        inZoom = false;
    }

    public void showDocument() {
        // use post to ensure that document view has width and height before decoding begin
        post(() -> {
            init();
            updatePageVisibility();
        });
    }

    public void goToPage(int toPage) {
        if (isInitialized) {
            goToPageImpl(toPage);
        } else {
            pageToGoTo = toPage;
        }
    }

    public void goToPage(int toPage, int scrollX, int scrollY) {
        xToScroll = scrollX;
        yToScroll = scrollY;
        if (isInitialized) {
            goToPageImpl(toPage);
        } else {
            pageToGoTo = toPage;
        }
    }

    public int getCurrentPage() {
        //for (Map.Entry<Integer, Page> entry : pages.entrySet()) {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            if (page.isVisible()) {
                return pages.keyAt(i);
            }
        }
        return 0;
    }

    public Page getEventPage(MotionEvent e) {
        Page page = null;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            if (page.isVisible()) {
                if (oriention == VERTICAL) {
                    if ((page.bounds.top - getScrollY()) < e.getY() && (page.bounds.bottom - getScrollY()) > e.getY()) {
                        return page;
                    }
                } else {
                    if ((page.bounds.left - getScrollX()) < e.getX() && (page.bounds.right - getScrollX()) > e.getX()) {
                        return page;
                    }
                }
            }
        }
        return page;
    }

    public void zoomChanged(float newZoom, float oldZoom) {
        inZoom = true;
        stopScroller();
        if (!isInitialized) {
            return;
        }
        final float ratio = newZoom / oldZoom;
        invalidatePageSizes();
        scrollTo((int) ((getScrollX() + getWidth() / 2) * ratio - getWidth() / 2), (int) ((getScrollY() + getHeight() / 2) * ratio - getHeight() / 2));
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (multiTouchZoom != null) {
            if (multiTouchZoom.onTouchEvent(ev)) {
                return true;
            }

            if (multiTouchZoom.isResetLastPointAfterZoom()) {
                multiTouchZoom.setResetLastPointAfterZoom(false);
            }
        }

        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    lineByLineMoveTo(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    lineByLineMoveTo(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    verticalDpadScroll(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    verticalDpadScroll(-1);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void verticalDpadScroll(int direction) {
        mCurrentFlingRunnable = new FlingRunnable(getContext());
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);
        post(mCurrentFlingRunnable);
    }

    private void lineByLineMoveTo(int direction) {
        if (direction == 1 ? getScrollX() == getRightLimit() : getScrollX() == getLeftLimit()) {
            mCurrentFlingRunnable = new FlingRunnable(getContext());
            mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), direction * (getLeftLimit() - getRightLimit()), (int) (direction * pages.get(getCurrentPage()).bounds.height() / 50));
        } else {
            mCurrentFlingRunnable = new FlingRunnable(getContext());
            mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), direction * getWidth() / 2, 0);
        }
        post(mCurrentFlingRunnable);
    }

    private int getTopLimit() {
        return 0;
    }

    private int getLeftLimit() {
        return 0;
    }

    private int getBottomLimit() {
        if (oriention == HORIZONTAL) {
            return (int) (getHeight() * zoomModel.getZoom()) - getHeight();
        } else {
            return (int) pages.get(pages.size() - 1).bounds.bottom - getHeight();
        }
    }

    private int getRightLimit() {
        if (oriention == HORIZONTAL) {
            return (int) pages.get(pages.size() - 1).bounds.right - getWidth();
        } else {
            return (int) (getWidth() * zoomModel.getZoom()) - getWidth();
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()), Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
        viewRect = null;
    }

    RectF getViewRect() {
        if (viewRect == null) {
            viewRect = new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
        }
        return viewRect;
    }

    /**
     * 对于缩图,需要更多的预期高度,两个方向都要处理.
     * 有了缩略图,getViewRect()就可以不用预加载空间了.
     *
     * @return
     */
    RectF getViewRectForPage() {
        if (viewRect == null) {
            float width = getWidth();
            float height = getHeight();
            float left = getScrollX();
            float top = getScrollY();
            if (oriention == HORIZONTAL) {
                width = width * 1.8f;
                left = left - width * 0.2f;
            } else {
                height = height * 1.8f;
                top = top - height * 0.2f;
            }
            viewRect = new RectF(left, top, getScrollX() + width, getScrollY() + height);
        }
        return viewRect;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //for (Page page : pages.values()) {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.draw(canvas);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        float scrollScaleRatio = getScrollScaleRatio();
        invalidatePageSizes();
        invalidateScroll(scrollScaleRatio);
        commitZoom();
    }

    void invalidatePageSizes() {
        if (!isInitialized) {
            return;
        }
        if (oriention == HORIZONTAL) {
            float widthAccum = 0;
            int height = getHeight();
            float zoom = zoomModel.getZoom();
            for (int i = 0; i < pages.size(); i++) {
                Page page = pages.get(i);
                float pageWidth = page.getPageWidth(height, zoom);
                page.setBounds(new RectF(widthAccum, 0, widthAccum + pageWidth, height * zoom));
                widthAccum += pageWidth;
            }
        } else {
            float heightAccum = 0;
            int width = getWidth();
            float zoom = zoomModel.getZoom();
            for (int i = 0; i < pages.size(); i++) {
                Page page = pages.get(i);
                float pageHeight = page.getPageHeight(width, zoom);
                page.setBounds(new RectF(0, heightAccum, width * zoom, heightAccum + pageHeight));
                heightAccum += pageHeight;
            }
        }
    }

    private void invalidateScroll(float ratio) {
        if (!isInitialized) {
            return;
        }
        stopScroller();
        final Page page = pages.get(0);
        if (page == null || page.bounds == null) {
            return;
        }
        if (pageToGoTo > 0) {
            goToPageImpl(pageToGoTo);
        } else {
            scrollTo((int) (getScrollX() * ratio), (int) (getScrollY() * ratio));
        }
    }

    private float getScrollScaleRatio() {
        final Page page = pages.get(0);
        if (page == null || page.bounds == null) {
            return 0;
        }
        final float v = zoomModel.getZoom();
        if (oriention == HORIZONTAL) {
            return getHeight() * v / page.bounds.height();
        } else {
            return getWidth() * v / page.bounds.width();
        }
    }

    private void stopScroller() {
        cancelFling();
    }

    public ZoomModel getZoomModel() {
        return zoomModel;
    }

    public void setScrollMargin(int margin) {
        mMargin = margin;
    }

    public void setDecodePage(int decodePage) {
        preDecodePage = decodePage;
    }

    public void scrollPage(int height) {
        mCurrentFlingRunnable = new FlingRunnable(getContext());
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, height, 0);
        post(mCurrentFlingRunnable);
        //Log.d(VIEW_LOG_TAG, "height:" + height);
    }

    public boolean tryHyperlink(MotionEvent e) {
        if (decodeService.getPageCount() < 1) {
            return false;
        }

        Page page = getEventPage(e);
        if (null != page) {
            float scale = calculateScale(page);
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            float x = Math.abs((e.getX() + scrollX - page.bounds.left) / scale);
            float y = Math.abs((e.getY() + scrollY - page.bounds.top) / scale);
            //Log.d(VIEW_LOG_TAG, String.format("scrollX:%s, scrollY:%s, scale:%s, zoom:%s, index:%s, e.x:%s, e.y:%s, bound:%s",
            //        scrollX, scrollY, scale, zoomModel.getZoom(), page.index, e.getX(), e.getY(), page.bounds.top));

            Hyperlink link = Hyperlink.Companion.mapPointToPage(page, x, y);
            //Log.d(VIEW_LOG_TAG, String.format("x:%s, y:%s, bounds:%s, link:%s, links:%s", x, y, page.bounds, link, page.links));
            if (link != null) {
                if (Hyperlink.LINKTYPE_URL == link.getLinkType()) {
                    Hyperlink.Companion.openSystemBrowser(getContext(), link.getUrl());
                    return true;
                } else {
                    goToPage(link.getPage());
                    return true;
                }
            }
        }
        return false;
    }

    public float calculateScale(Page page) {
        CodecPage vuPage = decodeService.getPage(page.index);
        if (oriention == VERTICAL) {
            return zoomModel.getZoom() * (1.0f * getWidth() / vuPage.getWidth());
        } else {
            return zoomModel.getZoom() * (1.0f * getHeight() / vuPage.getHeight());
        }
    }

    private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (tryHyperlink(e)) {
                return true;
            }
            int height = getHeight();
            int top = height / 4;
            int bottom = height * 3 / 4;
            //Log.d(VIEW_LOG_TAG, "height:"+height+" y:"+e.getY()+" mMargin:"+mMargin);

            height = height - mMargin;
            if ((int) e.getY() < top) {
                scrollPage(-height);
            } else if ((int) e.getY() > bottom) {
                scrollPage(height);
            } else {
                //currentPageModel.dispatch(new CurrentPageListener.CurrentPageChangedEvent(getCurrentPage()));
                if (null != simpleGestureListener) {
                    Page page = getEventPage(e);
                    if (null != page) {
                        simpleGestureListener.onSingleTapConfirmed(page.index);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent ev) {
            if (ev.getEventTime() - lastDownEventTime < DOUBLE_TAP_TIME) {
                /*if (null != mPageModel) {
                    mPageModel.setCurrentPage(currentPageModel.getCurrentPageIndex());
                    mPageModel.setPageCount(decodeService.getPageCount());
                    mPageModel.toggleSeekControls();
                }*/
                //zoomModel.toggleZoomControls();
                if (null != simpleGestureListener) {
                    simpleGestureListener.onDoubleTapEvent(getCurrentPage());
                }
                return true;
            } else {
                lastDownEventTime = ev.getEventTime();
            }
            return false;
        }

        public boolean onDown(MotionEvent ev) {
            stopScroller();
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mCurrentFlingRunnable = new FlingRunnable(getContext());
            mCurrentFlingRunnable.fling(getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY, getLeftLimit(), getRightLimit(), getTopLimit(), getBottomLimit());
            post(mCurrentFlingRunnable);
            return true;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onScroll(MotionEvent ev, MotionEvent e2, float distanceX, float distanceY) {
            scrollBy((int) distanceX, (int) distanceY);
            return true;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }
    }
    //--------------------------------

    private FlingRunnable mCurrentFlingRunnable;

    private void cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private class FlingRunnable implements Runnable {

        public FlingRunnable(Context context) {
        }

        public void cancelFling() {
            scroller.forceFinished(true);
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        }

        public void startScroll(int startX, int startY, int dx, int dy) {
            startScroll(startX, startY, dx, dy, 0);
        }

        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            if (duration == 0) {
                scrollBy(dx, dy);
                DocumentView.this.invalidate();
            } else {
                scroller.startScroll(startX, startY, dx, dy, duration);
            }
        }

        @Override
        public void run() {
            if (scroller.isFinished()) {
                return; // remaining post that should not be handled
            }

            if (scroller.computeScrollOffset()) {
                scrollTo(scroller.getCurrX(), scroller.getCurrY());
                //postInvalidate();

                // Post On animation
                postOnAnimation(this);
            }
        }
    }
}
