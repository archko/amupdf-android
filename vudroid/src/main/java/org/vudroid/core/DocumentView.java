package org.vudroid.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.vudroid.core.codec.SearchResult;
import org.vudroid.core.events.ZoomListener;
import org.vudroid.core.models.CurrentPageModel;
import org.vudroid.core.models.DecodingProgressModel;
import org.vudroid.core.models.ZoomModel;
import org.vudroid.core.multitouch.MultiTouchZoom;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.common.AnnotationManager;
import cn.archko.pdf.core.entity.APage;
import cn.archko.pdf.core.entity.AnnotationPath;
import cn.archko.pdf.core.entity.DrawType;
import cn.archko.pdf.core.entity.Offset;
import cn.archko.pdf.core.entity.PathConfig;
import cn.archko.pdf.core.link.Hyperlink;
import cn.archko.pdf.core.utils.ColorUtil;

public class DocumentView extends View implements ZoomListener {

    public static final String TAG = "DocumentView";
    final ZoomModel zoomModel;
    private final CurrentPageModel currentPageModel;
    DecodeService decodeService;
    private final SparseArray<Page> pages = new SparseArray<>();
    private final List<Page> lastPages = new ArrayList<>();
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

    AnnotationManager annotationManager = null;
    private boolean selection;
    private boolean draw;

    // 选择模式相关状态
    private boolean isSelecting = false;
    private Page selectedPage = null;
    private float selectionStartX, selectionStartY;
    private float selectionEndX, selectionEndY;
    private float pageSelectionStartX, pageSelectionStartY;
    private float pageSelectionEndX, pageSelectionEndY;

    // 绘制模式相关状态
    private boolean isDrawing = false;
    private Page drawingPage = null;
    private final List<PointF> drawingPoints = new ArrayList<>();
    private Paint drawingPaint;
    private int drawColor = Color.RED;
    private float drawStrokeWidth = 4f;
    private DrawType drawType = DrawType.LINE;

    private final GestureDetector mGestureDetector;
    boolean crop = false;
    private final DocViewListener docViewListener;
    private int speakingPage = -1;

    float widthAccum = 0;

    float heightAccum = 0;

    public int getOriention() {
        return oriention;
    }

    public void setOriention(int oriention) {
        if (this.oriention != oriention) {
            pageToGoTo = getCurrentPage();
            this.oriention = oriention;
            zoomModel.setZoom(1f);
            BitmapCache.getInstance().clearNode();
            if (null != decodeService) {
                decodeService.setOriention(oriention);
            }

            Log.d(TAG, String.format("setOriention:%s, page:%s", oriention, pageToGoTo));

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

    public void setSelection(boolean selection) {
        if (this.selection != selection) {
            this.selection = selection;
            if (selection) {
                this.draw = false;
            }
            resetGestureState();
            invalidate();
            Log.d(TAG, "设置选择模式: " + selection);
        }
    }

    public void setDraw(boolean draw) {
        if (this.draw != draw) {
            this.draw = draw;
            if (draw) {
                this.selection = false;
            }
            resetGestureState();
            initDrawingPaint();
            invalidate();
            Log.d(TAG, "设置绘制模式: " + draw);
        }
    }

    public void setDrawConfig(int color, float strokeWidth, DrawType drawType) {
        this.drawColor = color;
        this.drawStrokeWidth = strokeWidth;
        this.drawType = drawType;
        initDrawingPaint();
        Log.d(TAG, String.format("设置绘制配置: color=%d, strokeWidth=%.1f, drawType=%s", color, strokeWidth, drawType));
    }

    public void setAnnotationManager(AnnotationManager annotationManager) {
        this.annotationManager = annotationManager;
    }

    private void resetGestureState() {
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                Page page = pages.valueAt(i);
                if (page != null) {
                    page.clearTextSelection();
                }
            }
        }

        isSelecting = false;
        isDrawing = false;
        selectedPage = null;
        drawingPage = null;
        drawingPoints.clear();
    }

    private void initDrawingPaint() {
        drawingPaint = new Paint();
        drawingPaint.setColor(drawColor);
        drawingPaint.setStrokeWidth(drawStrokeWidth);
        drawingPaint.setStyle(Paint.Style.STROKE);
        drawingPaint.setAntiAlias(true);
    }

    public DocumentView(Context context, final ZoomModel zoomModel,
                        int oriention, int scrollX, int scrollY,
                        DecodingProgressModel progressModel,
                        CurrentPageModel currentPageModel,
                        DocViewListener docViewListener) {
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
        this.docViewListener = docViewListener;

        setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
        Page page = pages.get(toPage);
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
        if (inZoom) {
            return;
        }
        // on scrollChanged can be called from scrollTo just after new layout applied so we should wait for relayout
        currentPageChanged();
        post(this::updatePageVisibility);
    }

    private void currentPageChanged() {
        post(() -> docViewListener.setCurrentPage(getCurrentPage()));
    }

    private void updatePageVisibility() {
        List<Page> visibleList = new ArrayList<>();

        int currentPage = binarySearchCurrentPage();
        if (currentPage == -1) {
            currentPage = 0;
        }

        // 向前遍历直到遇到不可见页
        for (int i = currentPage; i >= 0; i--) {
            Page page = pages.valueAt(i);
            if (page.isVisible()) {
                visibleList.add(0, page);
                page.updateVisibility();
            } else {
                break;
            }
        }

        // 向后遍历直到遇到不可见页
        for (int i = currentPage + 1; i < pages.size(); i++) {
            Page page = pages.valueAt(i);
            if (page.isVisible()) {
                visibleList.add(page);
                page.updateVisibility();
            } else {
                break;
            }
        }

        // 处理从可见变为不可见的页面
        for (Page page : lastPages) {
            if (!visibleList.contains(page)) {
                page.updateVisibility();
            }
        }

        lastPages.clear();
        lastPages.addAll(visibleList);
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
        //Log.d(TAG, String.format("getCurrentPage.current:%s, scrollY:%s", current, getScrollY()));
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

            if (current < pages.size() - 2) {
                for (int i = current + 1; i < pages.size(); i++) {
                    page = pages.valueAt(i);
                    if (page.isVisible()) {
                        return pages.keyAt(i);
                    }
                }
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
                if (page.bounds.bottom <= scrollY) {
                    low = middle + 1;
                } else if (page.bounds.top > scrollY) {
                    high = middle - 1;
                } else {
                    return middle;
                }
            }
        } else {
            while (low <= high) {
                middle = (low + high) / 2;
                page = pages.valueAt(middle);
                if (page.bounds.right <= scrollX) {
                    low = middle + 1;
                } else if (page.bounds.left > scrollX) {
                    high = middle - 1;
                } else {
                    return middle;
                }
            }
        }

        return -1;
    }

    public Page getEventPage(MotionEvent e) {
        if (decodeService.getPageCount() < 1) {
            return null;
        }

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
        scrollTo((int) ((getScrollX() + getWidth() / 2f) * ratio - getWidth() / 2f), (int) ((getScrollY() + getHeight() / 2f) * ratio - getHeight() / 2f));
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        if (!isInitialized) {
            return true;
        }

        // 根据当前模式处理触摸事件
        if (selection) {
            return handleSelectionTouch(ev);
        } else if (draw) {
            return handleDrawTouch(ev);
        } else {
            // 普通视图模式
            return handleViewModeTouch(ev);
        }
    }

    private boolean handleViewModeTouch(MotionEvent ev) {
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

    private boolean handleSelectionTouch(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return handleSelectionDown(x, y);
            case MotionEvent.ACTION_MOVE:
                return handleSelectionMove(x, y);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleSelectionUp();
        }
        return true;
    }

    private boolean handleDrawTouch(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return handleDrawDown(x, y);
            case MotionEvent.ACTION_MOVE:
                return handleDrawMove(x, y);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleDrawUp();
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
        mCurrentFlingRunnable = new FlingRunnable();
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), 0, direction * getHeight() / 2);
        post(mCurrentFlingRunnable);
    }

    private void lineByLineMoveTo(int direction) {
        mCurrentFlingRunnable = new FlingRunnable();
        if (direction == 1 ? getScrollX() == getRightLimit() : getScrollX() == getLeftLimit()) {
            mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), direction * (getLeftLimit() - getRightLimit()), (int) (direction * pages.get(getCurrentPage()).bounds.height() / 50));
        } else {
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
        } catch (Exception ignored) {

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
     * 对于缩略图,需要更多的预期高度,两个方向都要处理.
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
                width = width * 1.2f;
                left = left - width * 0.2f;
            } else {
                height = height * 1.2f;
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
            for (int i = current; i < pages.size(); i++) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    page.draw(canvas);
                } else {
                    break;
                }
            }

            for (int i = current; i >= 0; i--) {
                page = pages.valueAt(i);
                if (page.isVisible()) {
                    page.draw(canvas);
                } else {
                    break;
                }
            }
        }

        // 绘制选择区域和当前绘制的路径
        drawSelectionAndDrawing(canvas);
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

    public void scrollPage(int width, int height) {
        mCurrentFlingRunnable = new FlingRunnable();
        mCurrentFlingRunnable.startScroll(getScrollX(), getScrollY(), width, height, 0);
        post(mCurrentFlingRunnable);
        //Log.d(TAG, "height:" + height);
    }

    private Page tryHyperlink(MotionEvent e, Page page) {
        if (null != page) {
            // 使用Page的screenToPagePoint方法获取页面原始坐标
            float screenX = e.getX() + getScrollX();
            float screenY = e.getY() + getScrollY();
            PointF pagePoint = page.screenToPagePoint(screenX, screenY);
            float x = Math.abs(pagePoint.x);
            float y = Math.abs(pagePoint.y);

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

    protected List<SearchResult> searchResults = new ArrayList<>();

    public void prev(String text) {
        int curr = getCurrentPage();
        SearchResult closest = getClosest(curr);

        if (closest != null) {
            if (closest.page == curr) {
                int index = searchResults.indexOf(closest);
                // 当前页刚好是某个搜索结果，尝试跳转到上一个
                if (index > 0) {
                    goToPage(searchResults.get(index - 1).page, 0, 0);
                } else {
                    Log.d(TAG, "已经是第一个搜索结果");
                }
            } else {
                goToPage(closest.page, 0, 0);
            }
        }
    }

    @Nullable
    private SearchResult getClosest(int curr) {
        SearchResult closest = null;
        int minDiff = Integer.MAX_VALUE;
        for (SearchResult result : searchResults) {
            int diff = Math.abs(result.page - curr);
            if (diff < minDiff) {
                minDiff = diff;
                closest = result;
            }
        }
        return closest;
    }

    public void next(String text) {
        int curr = getCurrentPage();
        SearchResult closest = getClosest(curr);
        if (closest != null) {
            if (closest.page == curr) {
                int index = searchResults.indexOf(closest);
                if (index < searchResults.size() - 1) {
                    goToPage(searchResults.get(index + 1).page, 0, 0);
                }
            } else {
                goToPage(closest.page, 0, 0);
            }
        }
    }

    public int getSpeakingPage() {
        return speakingPage;
    }

    public void setSpeakingPage(int speakingPage) {
        this.speakingPage = speakingPage;
    }

    public SearchResult getSearchResult(int index) {
        for (SearchResult result : searchResults) {
            if (result.page == index) {
                return result;
            }
        }
        return null;
    }

    public void clearSearch() {
        searchResults.clear();
        updatePageSearchBox();
    }

    private void updatePageSearchBox() {
        Page page;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.valueAt(i);
            page.updatePageSearchBox();
        }
    }

    public void setSearchResult(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
        updatePageSearchBox();
    }

    private class MySimpleOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Page page = getEventPage(e);
            Page evPage = tryHyperlink(e, page);
            if (evPage != null) {
                return true;
            }
            if (null != docViewListener) {
                int index = 0;
                if (null != page) {
                    index = page.index;
                }
                docViewListener.onSingleTapConfirmed(e, index);
                return true;
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            if (null != docViewListener) {
                Page page = getEventPage(ev);
                int index = 0;
                if (null != page) {
                    index = page.index;
                }
                docViewListener.onDoubleTap(ev, index);
            }
            return false;
        }

        public boolean onDown(MotionEvent ev) {
            stopScroller();
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mCurrentFlingRunnable = new FlingRunnable();
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

    // 选择模式处理方法
    private boolean handleSelectionDown(float x, float y) {
        Page page = getEventPageFromScreen(x, y);
        if (page != null) {
            isSelecting = true;
            selectedPage = page;
            selectionStartX = x;
            selectionStartY = y;
            selectionEndX = x;
            selectionEndY = y;

            // 使用Page的screenToPagePoint方法获取页面原始坐标
            // 注意：需要将屏幕坐标转换为相对于文档内容的坐标
            float screenX = x + getScrollX();
            float screenY = y + getScrollY();
            PointF pagePoint = page.screenToPagePoint(screenX, screenY);
            pageSelectionStartX = pagePoint.x;
            pageSelectionStartY = pagePoint.y;
            pageSelectionEndX = pageSelectionStartX;
            pageSelectionEndY = pageSelectionStartY;

            Log.d(TAG, String.format("开始选择: 页面%d, 屏幕坐标(%.1f, %.1f), 页面原始坐标(%.1f, %.1f)",
                    page.index, x, y, pageSelectionStartX, pageSelectionStartY));
            invalidate();
            return true;
        }
        return false;
    }

    private boolean handleSelectionMove(float x, float y) {
        if (isSelecting && selectedPage != null) {
            selectionEndX = x;
            selectionEndY = y;

            float screenX = x + getScrollX();
            float screenY = y + getScrollY();
            PointF pagePoint = selectedPage.screenToPagePoint(screenX, screenY);
            pageSelectionEndX = pagePoint.x;
            pageSelectionEndY = pagePoint.y;

            if (decodeService != null) {
                List<RectF> selectionRects = decodeService.getTextSelectionRects(
                        selectedPage.index,
                        pageSelectionStartX, pageSelectionStartY,
                        pageSelectionEndX, pageSelectionEndY
                );

                if (selectionRects != null) {
                    // 将页面坐标的矩形转换为屏幕坐标
                    List<RectF> screenRects = new ArrayList<>();
                    for (RectF pageRect : selectionRects) {
                        RectF screenRect = selectedPage.getPageRegion(selectedPage.bounds, pageRect);
                        screenRects.add(screenRect);
                    }
                    selectedPage.setSelectionRects(screenRects);
                }
            }

            //Log.d(TAG, String.format("更新选择: 页面%d, 屏幕坐标(%.1f, %.1f), 页面原始坐标(%.1f, %.1f)",
            //        selectedPage.index, x, y, pageSelectionEndX, pageSelectionEndY));

            invalidate();
            return true;
        }
        return false;
    }

    private boolean handleSelectionUp() {
        if (isSelecting && selectedPage != null) {
            String selectedText = null;
            if (decodeService != null) {
                selectedText = decodeService.getSelectedText(
                        selectedPage.index,
                        pageSelectionStartX, pageSelectionStartY,
                        pageSelectionEndX, pageSelectionEndY
                );
            }

            Log.d(TAG, String.format("结束选择: 页面%d, 选择区域(%.1f, %.1f) -> (%.1f, %.1f), 选中文本: %s",
                    selectedPage.index, selectionStartX, selectionStartY, selectionEndX, selectionEndY,
                    selectedText != null ? selectedText : "null"));

            // 重置选择状态
            isSelecting = false;
            selectedPage = null;
            invalidate();
            return true;
        }
        return false;
    }

    // 绘制模式处理方法
    private boolean handleDrawDown(float x, float y) {
        Page page = getEventPageFromScreen(x, y);
        if (page != null) {
            isDrawing = true;
            drawingPage = page;
            drawingPoints.clear();

            float screenX = x + getScrollX();
            float screenY = y + getScrollY();
            PointF relativePoint = page.screenToPageRelativePoint(screenX, screenY);
            PointF point = new PointF(relativePoint.x, relativePoint.y);
            drawingPoints.add(point);  // 起点

            Log.d(TAG, String.format("开始绘制: 页面%d, 模式%s, 相对坐标(%.3f, %.3f)",
                    page.index, drawType, relativePoint.x, relativePoint.y));

            invalidate();
            return true;
        }
        return false;
    }

    private boolean handleDrawMove(float x, float y) {
        if (isDrawing && drawingPage != null) {
            float screenX = x + getScrollX();
            float screenY = y + getScrollY();
            PointF relativePoint = drawingPage.screenToPageRelativePoint(screenX, screenY);

            if (drawType == DrawType.LINE) {
                // LINE模式: 只保留起点和当前终点
                if (drawingPoints.size() > 1) {
                    drawingPoints.set(1, new PointF(relativePoint.x, relativePoint.y));
                } else {
                    drawingPoints.add(new PointF(relativePoint.x, relativePoint.y));
                }
            } else {
                // CURVE模式: 记录所有点
                PointF point = new PointF(relativePoint.x, relativePoint.y);
                drawingPoints.add(point);
            }

            Log.d(TAG, String.format("继续绘制: 页面%d, 模式%s, 点%d, 相对坐标(%.3f, %.3f)",
                    drawingPage.index, drawType, drawingPoints.size(), relativePoint.x, relativePoint.y));

            invalidate();
            return true;
        }
        return false;
    }

    private boolean handleDrawUp() {
        if (isDrawing && drawingPage != null && drawingPoints.size() > 1) {
            Log.d(TAG, String.format("结束绘制: 页面%d, 共%d个点, 模式%s",
                    drawingPage.index, drawingPoints.size(), drawType));

            if (annotationManager != null) {
                try {
                    List<Offset> offsetList = new ArrayList<>();

                    if (drawType == DrawType.LINE) {
                        // LINE模式: 计算水平或垂直线的终点
                        PointF startPoint = drawingPoints.get(0);
                        PointF endPoint = drawingPoints.get(drawingPoints.size() - 1);

                        // 判断主要方向（水平还是垂直）
                        float dx = Math.abs(endPoint.x - startPoint.x);
                        float dy = Math.abs(endPoint.y - startPoint.y);

                        PointF finalEndPoint = new PointF(endPoint.x, endPoint.y);
                        if (dx > dy) {
                            // 水平线：使用起点的y坐标
                            finalEndPoint.y = startPoint.y;
                        } else {
                            // 垂直线：使用起点的x坐标
                            finalEndPoint.x = startPoint.x;
                        }

                        offsetList.add(new Offset(startPoint.x, startPoint.y));
                        offsetList.add(new Offset(finalEndPoint.x, finalEndPoint.y));

                        Log.d(TAG, String.format("LINE模式: 起点(%.3f, %.3f), 终点(%.3f, %.3f), 方向=%s",
                                startPoint.x, startPoint.y, finalEndPoint.x, finalEndPoint.y, dx > dy ? "水平" : "垂直"));
                    } else {
                        // CURVE模式: 保存所有点
                        for (PointF point : drawingPoints) {
                            offsetList.add(new Offset(point.x, point.y));
                        }
                    }

                    PathConfig config = new PathConfig(
                            drawColor,
                            drawStrokeWidth,
                            drawType
                    );

                    AnnotationPath annotationPath = new AnnotationPath(
                            offsetList,
                            config
                    );

                    annotationManager.addPath(drawingPage.index, annotationPath);

                    Log.d(TAG, String.format("成功保存标注到AnnotationManager: 页面%d, 点数%d", drawingPage.index, offsetList.size()));
                } catch (Exception e) {
                    Log.e(TAG, "保存标注失败", e);
                    e.printStackTrace();
                }
            } else {
                Log.w(TAG, "AnnotationManager未设置,无法保存标注");
            }

            // 重置绘制状态
            isDrawing = false;
            drawingPage = null;
            drawingPoints.clear();
            invalidate();
            return true;
        }
        return false;
    }

    // 辅助方法：从屏幕坐标获取页面
    private Page getEventPageFromScreen(float screenX, float screenY) {
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, screenX, screenY, 0);
        Page page = getEventPage(event);
        event.recycle();
        return page;
    }

    // 临时绘制,如果这里没有,则会导致画的过程不显示.绘制选择区域和绘制路径
    private void drawSelectionAndDrawing(Canvas canvas) {
        if (isSelecting && selectedPage != null) {
            drawSelectionRect(canvas);
        }

        if (isDrawing && drawingPage != null && drawingPoints.size() > 1) {
            drawCurrentPath(canvas);
        }
    }

    private void drawSelectionRect(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.argb(100, 0, 120, 215)); // 半透明蓝色
        paint.setStyle(Paint.Style.FILL);

        float left = Math.min(selectionStartX, selectionEndX);
        float top = Math.min(selectionStartY, selectionEndY);
        float right = Math.max(selectionStartX, selectionEndX);
        float bottom = Math.max(selectionStartY, selectionEndY);

        canvas.drawRect(left, top, right, bottom, paint);

        // 绘制边框
        paint.setColor(Color.rgb(0, 120, 215));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawRect(left, top, right, bottom, paint);
    }

    private void drawCurrentPath(Canvas canvas) {
        if (drawingPaint == null || drawingPoints.size() < 2) return;

        // 使用与 Page.getPageRegion 相同的坐标转换逻辑
        float scale = calculateScale(drawingPage);

        if (drawType == DrawType.LINE && drawingPoints.size() == 2) {
            // LINE模式: 绘制水平或垂直线
            PointF startPoint = drawingPoints.get(0);
            PointF endPoint = drawingPoints.get(1);

            // 计算水平或垂直线的终点（预览时也保持这个逻辑）
            float dx = Math.abs(endPoint.x - startPoint.x);
            float dy = Math.abs(endPoint.y - startPoint.y);

            PointF finalEndPoint = new PointF(endPoint.x, endPoint.y);
            if (dx > dy) {
                finalEndPoint.y = startPoint.y;
            } else {
                finalEndPoint.x = startPoint.x;
            }

            // 将相对坐标转换为屏幕坐标（考虑缩放和滚动）
            float startX = convertRelativeToScreen(startPoint.x, startPoint.y, scale).x;
            float startY = convertRelativeToScreen(startPoint.x, startPoint.y, scale).y;
            float endX = convertRelativeToScreen(finalEndPoint.x, finalEndPoint.y, scale).x;
            float endY = convertRelativeToScreen(finalEndPoint.x, finalEndPoint.y, scale).y;

            canvas.drawLine(startX, startY, endX, endY, drawingPaint);
        } else {
            // CURVE模式: 绘制曲线
            Path path = new Path();

            PointF firstScreen = convertRelativeToScreen(drawingPoints.get(0).x, drawingPoints.get(0).y, scale);
            path.moveTo(firstScreen.x, firstScreen.y);

            for (int i = 1; i < drawingPoints.size(); i++) {
                PointF screen = convertRelativeToScreen(drawingPoints.get(i).x, drawingPoints.get(i).y, scale);
                path.lineTo(screen.x, screen.y);
            }

            canvas.drawPath(path, drawingPaint);
        }
    }

    /**
     * 将页面相对坐标(0-1)转换为屏幕绘制坐标
     * 与 Page.getPageRegion 使用相同的转换逻辑
     * 在 DocumentView.onDraw 中调用时，canvas 坐标需要减去滚动偏移
     */
    private PointF convertRelativeToScreen(float relativeX, float relativeY, float scale) {
        // 获取原始页面尺寸
        APage vuPage = decodeService.getAPage(drawingPage.index);
        if (vuPage == null) {
            return new PointF(0, 0);
        }

        // 将相对坐标转换为页面原始坐标
        float pageX = relativeX * vuPage.getWidth(false);
        float pageY = relativeY * vuPage.getHeight(false);

        // 处理切边
        if (crop) {
            Rect rect = getBounds(drawingPage);
            if (rect != null) {
                pageX -= rect.left;
                pageY -= rect.top;
            }
        }

        // 应用缩放
        float scaledX = pageX * scale;
        float scaledY = pageY * scale;

        // 转换为文档坐标（相对于 pageBounds）
        float docX = scaledX + drawingPage.bounds.left;
        float docY = scaledY + drawingPage.bounds.top;

        // 在 DocumentView.onDraw 中绘制时，需要转换为屏幕坐标（减去滚动偏移）
        float screenX = docX - getScrollX();
        float screenY = docY - getScrollY();

        return new PointF(screenX, screenY);
    }

    public void cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private class FlingRunnable implements Runnable {

        public FlingRunnable() {
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
                return;
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
