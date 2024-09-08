package org.vudroid.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.vudroid.core.events.ZoomListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.multitouch.MultiTouchZoom;

import cn.archko.pdf.core.entity.APage;
import cn.archko.pdf.core.link.Hyperlink;
import cn.archko.pdf.core.listeners.SimpleGestureListener;
import cn.archko.pdf.core.utils.ColorUtil;

public class DocumentView extends View implements ZoomListener {

    public static final String TAG = "DocumentView";
    final ZoomModel zoomModel;
    private final CurrentPageModel currentPageModel;
    DecodeService decodeService;
    private final SparseArray<Page> pages = new SparseArray<>();
    private boolean isInitialized = false;
    private int pageToGoTo = -1;
    private int xToScroll;
    private int yToScroll;
    private final Flinger scroller;
    DecodingProgressModel progressModel;
    private RectF viewRect;
    private boolean inZoom;
    private MultiTouchZoom multiTouchZoom;
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;
    private int oriention = VERTICAL;
    private ColorFilter filter;

    private final GestureDetector mGestureDetector;
    boolean crop = false;
    private SimpleGestureListener simpleGestureListener;

    float widthAccum = 0;

    float heightAccum = 0;

    public int getOriention() {
        return oriention;
    }

    public void setOriention(int oriention) {
        if (this.oriention != oriention) {
            this.oriention = oriention;
            pageToGoTo = getCurrentPage();
            if (null != decodeService) {
                decodeService.setOriention(oriention);
            }

            //should invalidate current page and node

            requestLayout();
        }
    }

    public void setCrop(boolean crop) {
        if (this.crop != crop) {
            this.crop = crop;
            isInitialized = false;
            pageToGoTo = getCurrentPage();
            init();
        }
    }

    public DocumentView(Context context, final ZoomModel zoomModel,
                        int oriention, int scrollX, int scrollY,
                        DecodingProgressModel progressModel,
                        CurrentPageModel currentPageModel,
                        SimpleGestureListener simpleGestureListener) {
        super(context);
        this.zoomModel = zoomModel;
        this.oriention = oriention;
        this.xToScroll = scrollX;
        this.yToScroll = scrollY;
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
            final int width = decodeService.getEffectivePagesWidth(i, crop);
            final int height = decodeService.getEffectivePagesHeight(i, crop);
            Page page = new Page(this, i, crop, filter);
            pages.put(i, page);
            page.setAspectRatio(width, height);
        }
        Log.d(TAG, String.format("DecodeService:%s, pageToGoTo:%s, w-h:%s-%s", pages.size(), pageToGoTo, getWidth(), getHeight()));
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

        //因为切边的操作,导致有些页面比较小.所以底部加两个页面,滚动就可以正常了
        Page nextPage = null;
        if (pages.size() > toPage + 1) {
            nextPage = pages.get(toPage + 1);
            if (nextPage.getBottom() - nextPage.getTop() < getHeight()) {
                if (pages.size() > toPage + 2) {
                    nextPage = pages.get(toPage + 2);
                }
            }
        }
        int scrollX = 0;
        int scrollY = 0;
        if (oriention == VERTICAL) {
            scrollX = getScrollX();
            scrollY = page.getTop();
            if (xToScroll != 0) {
                scrollX = xToScroll;
            }
            if (yToScroll != 0) {
                Page bottomPage = nextPage == null ? page : nextPage;
                if (bottomPage.getBottom() > yToScroll) {
                    scrollY = yToScroll;
                }
            }
        } else {
            scrollX = page.getLeft();
            scrollY = getScrollY();
            if (yToScroll != 0) {
                scrollY = yToScroll;
            }
            if (xToScroll != 0) {
                Page bottomPage = nextPage == null ? page : nextPage;
                if (bottomPage.getRight() > xToScroll) {
                    scrollX = xToScroll;
                }
            }
        }
        Log.d(TAG, String.format("goToPageImpl.xToScroll:%s, scroll:%s, yToScroll:%s, scrollY:%s, bottom:%s, page:%s",
                xToScroll, scrollX, yToScroll, scrollY, page.getBottom(), page));
        yToScroll = 0;
        xToScroll = 0;

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
        //post(() -> {
        //    currentPageModel.setCurrentPage(getCurrentPage());
        //});
    }

    private void updatePageVisibility() {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.updateVisibility();
        }
    }

    public void commitZoom() {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.invalidate();
        }
        inZoom = false;
    }

    public void showDocument(boolean crop) {
        this.crop = crop;
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
        Page page;
        int current = binarySearchCurrentPage();
        if (current == -1) {
            for (int i = 0; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    return pages.keyAt(i);
                }
            }
        } else {
            for (int i = current; i >= 0; i--) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    return pages.keyAt(i);
                }
            }

            for (int i = current; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    return pages.keyAt(i);
                }
            }
        }
        return 0;
    }

    public int getLastVisiblePage() {
        Page page;
        for (int i = pages.size() - 1; i >= 0; i--) {
            page = pages.valueAt(i);
            if (page.isVisible()) {
                return pages.keyAt(i);
            }
        }
        return 0;
    }

    public int binarySearchCurrentPage() {
        int low = 0;
        int high = pages.size() - 1;
        int middle = 0;

        if (low > high) {
            return -1;
        }

        Page page = null;
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        if (oriention == VERTICAL) {
            while (low <= high) {
                middle = (low + high) / 2;
                page = pages.valueAt(middle);
                if (page.bounds.top > scrollY) {
                    high = middle - 1;
                } else if (page.bounds.top < scrollY) {
                    if (page.bounds.bottom > scrollY) {
                        return middle;
                    }
                    low = middle + 1;
                } else {
                    return middle;
                }
            }
        } else {
            while (low <= high) {
                middle = (low + high) / 2;
                page = pages.valueAt(middle);
                if (page.bounds.left > scrollX) {
                    high = middle - 1;
                } else if (page.bounds.left < scrollX) {
                    if (page.bounds.right > scrollX) {
                        return middle;
                    }
                    low = middle + 1;
                } else {
                    return middle;
                }
            }
        }

        return -1;
    }

    public Page getEventPage(MotionEvent e) {
        Page page = null;

        float evtX = e.getX();
        float evtY = e.getY();

        int scrollX = getScrollX();
        int scrollY = getScrollY();

        int current = binarySearchCurrentPage();
        if (current == -1) {
            for (int i = 0; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    if (oriention == VERTICAL) {
                        if ((page.bounds.top - scrollY) < evtY && (page.bounds.bottom - scrollY) > evtY) {
                            return page;
                        }
                    } else {
                        if ((page.bounds.left - scrollX) < evtX && (page.bounds.right - scrollX) > evtX) {
                            return page;
                        }
                    }
                }
            }
        } else {
            for (int i = current; i >= 0; i--) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    if (oriention == VERTICAL) {
                        if ((page.bounds.top - scrollY) < evtY && (page.bounds.bottom - scrollY) > evtY) {
                            return page;
                        }
                    } else {
                        if ((page.bounds.left - scrollX) < evtX && (page.bounds.right - scrollX) > evtX) {
                            return page;
                        }
                    }
                }
            }

            for (int i = current; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    if (oriention == VERTICAL) {
                        if ((page.bounds.top - scrollY) < evtY && (page.bounds.bottom - scrollY) > evtY) {
                            return page;
                        }
                    } else {
                        if ((page.bounds.left - scrollX) < evtX && (page.bounds.right - scrollX) > evtX) {
                            return page;
                        }
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
        try {
            super.scrollTo(Math.min(Math.max(x, getLeftLimit()), getRightLimit()), Math.min(Math.max(y, getTopLimit()), getBottomLimit()));
        } catch (Exception e) {

        }
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
        Page page;
        int current = binarySearchCurrentPage();
        if (current == -1) {
            for (int i = 0; i < pages.size(); i++) {
                page = pages.valueAt(i);
                page.draw(canvas);
            }
        } else {
            for (int i = current; i >= 0; i--) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    page.draw(canvas);
                } else {
                    break;
                }
            }

            for (int i = current; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    page.draw(canvas);
                } else {
                    break;
                }
            }
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
            widthAccum = 0;
            int height = getHeight();
            float zoom = zoomModel.getZoom();
            for (int i = 0; i < pages.size(); i++) {
                Page page = pages.get(i);
                float pageWidth = page.getPageWidth(height, zoom);
                page.setBounds(new RectF(widthAccum, 0, widthAccum + pageWidth, height * zoom));
                widthAccum += pageWidth;
            }
        } else {
            heightAccum = 0;
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
        if (pageToGoTo >= 0) {
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

    public void scrollPage(int height) {
        mCurrentFlingRunnable = new FlingRunnable(getContext());
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, height, 0);
        post(mCurrentFlingRunnable);
        //Log.d(TAG, "height:" + height);
    }

    private Page tryHyperlink(MotionEvent e) {
        if (decodeService.getPageCount() < 1) {
            return null;
        }

        Page page = getEventPage(e);
        if (null != page) {
            float scale = calculateScale(page);
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            float x = Math.abs((e.getX() + scrollX - page.bounds.left) / scale);
            float y = Math.abs((e.getY() + scrollY - page.bounds.top) / scale);

            if (crop) {
                Rect rect = getBounds(page);
                if (null != rect) {
                    x += rect.left;
                    y += rect.top;
                }
            }

            //Log.d(TAG, String.format("scrollX:%s, scrollY:%s, scale:%s, zoom:%s, index:%s, e.x:%s, e.y:%s, bound:%s",
            //        scrollX, scrollY, scale, zoomModel.getZoom(), page.index, e.getX(), e.getY(), page.bounds.top));

            Hyperlink link = Hyperlink.Companion.mapPointToPage(page, x, y);
            //Log.d(TAG, String.format("x:%s, y:%s, bounds:%s, link:%s, links:%s", x, y, page.bounds, link, page.links));
            if (link != null) {
                if (Hyperlink.LINKTYPE_URL == link.getLinkType()) {
                    Hyperlink.Companion.openSystemBrowser(getContext(), link.getUrl());
                    return page;
                } else {
                    goToPage(link.getPage());
                    return page;
                }
            }
        }
        return null;
    }

    public Rect getBounds(Page page) {
        APage vuPage = decodeService.getAPage(page.index);
        return vuPage.getCropBounds();
    }

    public float calculateScale(Page page) {
        APage vuPage = decodeService.getAPage(page.index);
        if (oriention == VERTICAL) {
            return zoomModel.getZoom() * (1.0f * getWidth() / vuPage.getWidth(crop));
        } else {
            return zoomModel.getZoom() * (1.0f * getHeight() / vuPage.getHeight(crop));
        }
    }

    public int getPage() {
        if (oriention == HORIZONTAL) {
            if (widthAccum == 0) {
                return 0;
            }
            int offset = getScrollX();
            int index = (int) (offset / widthAccum * pages.size());
            if (index < 0) {
                index = 0;
            } else if (index >= pages.size()) {
                index = pages.size() - 1;
            }
            Page page = pages.get(index);

            if (page.getBottom() > offset) {
                for (int i = index; i < pages.size(); i++) {
                    page = pages.get(i);
                    if (offset >= page.getLeft() && offset <= page.getRight()) {
                        return i;
                    }
                }
            } else {
                for (int i = index; i >= 0; i--) {
                    page = pages.get(i);
                    if (offset >= page.getLeft() && offset <= page.getRight()) {
                        return i;
                    }
                }
            }
        } else {
            if (heightAccum == 0) {
                return -1;
            }
            int offset = getScrollY();
            int index = (int) (offset / heightAccum * pages.size());
            if (index < 0) {
                index = 0;
            } else if (index >= pages.size()) {
                index = pages.size() - 1;
            }
            Page page = pages.get(index);

            if (page.getBottom() < offset) {
                for (int i = index; i < pages.size(); i++) {
                    page = pages.get(i);
                    if (offset >= page.getTop() && offset <= page.getBottom()) {
                        return i;
                    }
                }
            } else {
                for (int i = index; i >= 0; i--) {
                    page = pages.get(i);
                    if (offset >= page.getTop() && offset <= page.getBottom()) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public int getPageCount() {
        return decodeService.getPageCount();
    }

    public void applyFilter(int colorMode) {
        setFilter(colorMode);
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.applyFilter(filter);
        }
    }

    public void setFilter(int colorMode) {
        float[] colorMatrix = ColorUtil.getColorMode(colorMode);
        if (null == colorMatrix) {
            filter = null;
        } else {
            filter = new ColorMatrixColorFilter(new ColorMatrix(colorMatrix));
        }
    }

    private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Page page = tryHyperlink(e);
            if (page != null) {
                return true;
            }
            if (null != simpleGestureListener) {
                int index = 0;
                if (null != page) {
                    index = page.index;
                }
                simpleGestureListener.onSingleTapConfirmed(e, index);
                return true;
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            if (null != simpleGestureListener) {
                Page page = getEventPage(ev);
                int index = 0;
                if (null != page) {
                    index = page.index;
                }
                simpleGestureListener.onDoubleTap(ev, index);
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

    public void cancelFling() {
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
