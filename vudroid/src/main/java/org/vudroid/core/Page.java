package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;

import org.vudroid.R;
import org.vudroid.core.codec.PageTextBox;
import org.vudroid.core.codec.SearchResult;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.common.AnnotationManager;
import cn.archko.pdf.core.entity.APage;
import cn.archko.pdf.core.entity.AnnotationPath;
import cn.archko.pdf.core.entity.DrawType;
import cn.archko.pdf.core.entity.Offset;
import cn.archko.pdf.core.entity.PathConfig;
import cn.archko.pdf.core.link.Hyperlink;

public class Page {
    final int index;
    RectF bounds;
    private PageTreeNode node;
    private DocumentView documentView;
    public List<Hyperlink> links;
    private final TextPaint textPaint = textPaint();
    private Paint fillPaint = null;
    private final Paint strokePaint = strokePaint();
    private final Paint linkPaint = linkPaint();
    private final Paint searchPaint = searchPaint();
    private RectF speakingRect = new RectF();
    private final Paint speakingPaint = speakingPaint();
    public static final int ZOOM_THRESHOLD = 2;
    private boolean decodingNow;
    private Bitmap bitmap;
    private SoftReference<Bitmap> bitmapWeakReference;
    private boolean invalidateFlag;
    protected boolean crop = true;
    private ColorFilter filter;
    private List<PageTextBox> searchBoxs;
    private List<RectF> searchRectFs;

    // 文本选择相关 - 由DocumentView通过DecodeService处理
    private List<RectF> selectionRects;
    private Paint selectionPaint;

    Page(DocumentView documentView, int index, boolean crop, ColorFilter filter) {
        this.documentView = documentView;
        this.index = index;
        this.crop = crop;
        this.filter = filter;
        fillPaint = fillPaint();
        node = new PageTreeNode(documentView, new RectF(0, 0, 1, 1), this, ZOOM_THRESHOLD, null, filter);
        initSelectionPaint();
    }

    private float aspectRatio;

    float getPageHeight(int mainWidth, float zoom) {
        return mainWidth / getAspectRatio() * zoom;
    }

    float getPageWidth(int mainHeight, float zoom) {
        return mainHeight * getAspectRatio() * zoom;
    }

    public int getTop() {
        return Math.round(bounds.top);
    }

    public int getBottom() {
        return Math.round(bounds.bottom);
    }

    public int getLeft() {
        return Math.round(bounds.left);
    }

    public int getRight() {
        return Math.round(bounds.right);
    }

    public void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }
        //canvas.drawRect(bounds, fillPaint);
        Bitmap thumb = getBitmap();
        if (thumb != null && !thumb.isRecycled()) {
            //Matrix matrix = new Matrix();
            //matrix.postTranslate(bounds.left, bounds.top);
            //matrix.postScale(bounds.width()/thumb.getWidth(), bounds.height()/thumb.getHeight());
            //canvas.drawBitmap(thumb, matrix, null);
            Rect src = new Rect(0, 0, thumb.getWidth(), thumb.getHeight());
            Rect dst = new Rect((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
            canvas.drawBitmap(thumb, src, dst, fillPaint);

            //String text = String.format("Page%s,%s-%s,%s-%s,%s-%s, w-h:%s-%s",
            //        (index + 1), src.width(), src.height(),
            //        dst.left, dst.top, dst.right, bounds.bottom, dst.width(), dst.height());
            //canvas.drawText(text, bounds.centerX(), bounds.centerY(), textPaint);
        } else {
            canvas.drawText("Page:" + (index + 1), bounds.centerX(), bounds.centerY(), textPaint);
        }

        node.draw(canvas);
        //canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint);
        canvas.drawLine(bounds.left, bounds.bottom, bounds.right / 5, bounds.bottom, strokePaint);
        drawPageLinks(canvas);
        drawSearchResult(canvas);
        drawSpeaking(canvas);
        drawTextSelection(canvas); // 添加文本选择绘制
        drawAnnotations(canvas); // 添加标注绘制
    }

    protected String getKey() {
        return String.format("%s-%s", index, documentView.decodeService);
    }

    private Paint strokePaint() {
        final Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        return paint;
    }

    private Paint linkPaint() {
        final Paint paint = new Paint();
        paint.setColor(Color.parseColor("#80FFFF00"));
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    private Paint searchPaint() {
        final Paint paint = new Paint();
        paint.setColor(Color.parseColor("#50FF9800"));
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    private Paint speakingPaint() {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        return paint;
    }

    private Paint fillPaint() {
        final Paint fillPaint = new Paint();
        //fillPaint.setColor(Color.GRAY);
        fillPaint.setColor(Color.WHITE);    //scroll back show white bg
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColorFilter(filter);
        return fillPaint;
    }

    private Paint selectionPaint() {
        final Paint paint = new Paint();
        paint.setColor(Color.parseColor("#6633B5E5"));
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    private void initSelectionPaint() {
        selectionPaint = selectionPaint();
        selectionRects = new ArrayList<>();
    }

    private TextPaint textPaint() {
        final TextPaint paint = new TextPaint();
        paint.setColor(Color.BLUE);
        paint.setAntiAlias(true);
        paint.setTextSize(45);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public void applyFilter(ColorFilter filter) {
        this.filter = filter;
        fillPaint.setColorFilter(filter);
        node.applyFilter(filter);
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            float abs = Math.abs(aspectRatio - this.aspectRatio);
            boolean changed = this.aspectRatio != 0f && abs > 0.008;
            this.aspectRatio = aspectRatio;
            if (changed) {
                //Log.d("TAG", "setAspectRatio:" + this.aspectRatio + ", " + aspectRatio + ", " + abs);
                documentView.invalidatePageSizes();
            }
        }
    }

    public boolean isVisible() {
        if (null == bounds) {
            return false;
        }
        return RectF.intersects(documentView.getViewRectForPage(), bounds);
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0f / height);
    }

    void setBounds(RectF pageBounds) {
        bounds = pageBounds;
        node.invalidateNodeBounds();

        updatePageSearchBox();
    }

    public void updateVisibility() {
        if (null == bounds) { //tts后台滚动的时候会出现
            return;
        }

        if (isVisible()) {
            if (getBitmap() != null && !invalidateFlag) {
                restoreBitmapReference();
            } else {
                if (!crop) {
                    decodePageThumb();
                }
            }
            node.updateVisibility();
        } else {
            recycle();
        }
    }

    public void updatePageSearchBox() {
        searchBoxs = null;
        SearchResult result = documentView.getSearchResult(index);
        if (null != result && result.boxes != searchBoxs) {
            searchBoxs = result.boxes;
            initSearchRectFs(result.boxes);
        } else {
            searchBoxs = null;
            searchRectFs = null;
        }
    }

    private void initSearchRectFs(List<PageTextBox> boxes) {
        if (searchRectFs == null) {
            searchRectFs = new ArrayList<>();
        } else {
            searchRectFs.clear();
        }
        if (boxes != null && !boxes.isEmpty()) {
            for (PageTextBox rectF : boxes) {
                if (rectF.page == index) {
                    final RectF rect = getPageRegion(bounds, new RectF(rectF));
                    //Logcat.d(String.format("result:%s, rect:%s, %s", index, bounds, rectF));
                    searchRectFs.add(rect);
                }
            }
        }
    }

    private void recycle() {
        stopDecodingThisNode();
        setBitmap(null);
        node.recycleChildren();
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = bitmapWeakReference != null ? bitmapWeakReference.get() : null;
        if (null == bitmap) {
            bitmap = BitmapCache.getInstance().getBitmap(getKey());
        }
        return bitmap;
    }

    private void restoreBitmapReference() {
        setBitmap(getBitmap());
    }

    private final DecodeService.DecodeCallback decodeCallback = new DecodeService.DecodeCallback() {
        @Override
        public void decodeComplete(Bitmap bitmap, boolean isThumb, Object args) {
            setBitmap(bitmap);
            invalidateFlag = false;
            setDecodingNow(false);
        }

        @Override
        public boolean shouldRender(int pageNumber, boolean isFullPage) {
            if (getBitmap() != null) {
                return false;
            }
            //Log.d("TAG", "shouldRender:" + pageNumber);
            boolean isVisible = isVisible();
            if (!isVisible) {
                setBitmap(null);
                setDecodingNow(false);
            }
            return isVisible;
        }
    };

    private void decodePageThumb() {
        if (isDecodingNow()) {
            return;
        }
        setDecodingNow(true);
        documentView.decodeService.decodePage(
                getKey(),
                null,
                crop,
                index,
                decodeCallback,
                documentView.zoomModel.getZoom(),
                null);
    }

    private void setBitmap(Bitmap newBitmap) {
        if (newBitmap == null ||
                (newBitmap != null && newBitmap.getWidth() == -1 && newBitmap.getHeight() == -1)) {
            if (bitmap != null) {
                bitmapWeakReference.clear();
            }
            bitmap = null;
            return;
        }

        if (bitmap != newBitmap) {
            if (bitmap != null) {
                //BitmapPool.getInstance().release(bitmap);
                bitmapWeakReference.clear();
            }
            bitmapWeakReference = new SoftReference<>(newBitmap);
            documentView.postInvalidate();

            bitmap = newBitmap;
        }
    }

    private boolean isDecodingNow() {
        return decodingNow;
    }

    private void setDecodingNow(boolean decodingNow) {
        if (this.decodingNow != decodingNow) {
            this.decodingNow = decodingNow;
        }
    }

    private void stopDecodingThisNode() {
        if (!isDecodingNow()) {
            return;
        }
        documentView.decodeService.stopDecoding(getKey());
        setDecodingNow(false);
    }

    public void invalidate() {
        node.invalidate();
    }

    private void drawPageLinks(Canvas canvas) {
        if (null == links || links.isEmpty()) {
            return;
        }

        for (final Hyperlink link : links) {
            final RectF rect = getLinkSourceRect(bounds, link);
            if (rect != null) {
                if (link.getLinkType() == Hyperlink.LINKTYPE_PAGE) {
                    linkPaint.setColor(documentView.getContext().getResources().getColor(R.color.link_page));
                } else {
                    linkPaint.setColor(documentView.getContext().getResources().getColor(R.color.link_uri));
                }
                canvas.drawRect(rect, linkPaint);
            }
        }
    }

    private void drawSearchResult(Canvas canvas) {
        if (searchRectFs != null) {
            for (RectF rect : searchRectFs) {
                canvas.drawRect(rect, searchPaint);
            }
        }
    }

    private void drawSpeaking(Canvas canvas) {
        if (documentView.getSpeakingPage() == index) {
            speakingRect.set(bounds.left + 3, bounds.top + 3, bounds.right - 3, bounds.bottom - 3);
            canvas.drawRect(speakingRect, speakingPaint);
        }
    }

    public RectF getTargetRect(final RectF pageBounds, final RectF normalizedRect) {
        final Matrix tmpMatrix = new Matrix();

        tmpMatrix.postTranslate(pageBounds.left, pageBounds.top);

        final RectF targetRectF = new RectF();
        tmpMatrix.mapRect(targetRectF, normalizedRect);

        //MathUtils.floor(targetRectF);

        return targetRectF;
    }

    public RectF getLinkSourceRect(final RectF pageBounds, final Hyperlink link) {
        if (link == null || link.getBbox() == null) {
            return null;
        }
        return getPageRegion(pageBounds, new RectF(link.getBbox()));
    }

    public RectF getPageRegion(final RectF pageBounds, final RectF sourceRect) {
        final Matrix m = new Matrix();
        float scale = documentView.calculateScale(this);

        if (crop) {
            Rect rect = documentView.getBounds(this);
            if (null != rect) {
                sourceRect.offset(-rect.left, -rect.top);
            }
        }

        m.postScale(scale, scale);
        m.mapRect(sourceRect);

        return getTargetRect(pageBounds, sourceRect);
    }

    // 绘制标注
    private void drawAnnotations(Canvas canvas) {
        AnnotationManager annotationManager = documentView.annotationManager;
        if (annotationManager == null) {
            return;
        }

        // 获取当前页面的标注列表
        List<AnnotationPath> paths = annotationManager.getAnnotations().get(index);
        if (paths == null || paths.isEmpty()) {
            return;
        }

        // 绘制每个标注路径
        for (AnnotationPath annotationPath : paths) {
            drawSingleAnnotationPath(canvas, annotationPath);
        }
    }

    /**
     * 绘制单个标注路径
     */
    private void drawSingleAnnotationPath(Canvas canvas, AnnotationPath annotationPath) {
        List<Offset> points = annotationPath.getPoints();
        if (points == null || points.size() < 2) {
            return;
        }

        // 获取路径配置
        PathConfig config = annotationPath.getConfig();
        if (config == null) {
            return;
        }

        // 创建画笔
        Paint paint = new Paint();
        int color = config.getColor();
        paint.setColor(color);
        paint.setStrokeWidth(config.getStrokeWidth());
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // 获取缩放因子
        float scale = documentView.calculateScale(this);

        // 根据绘制类型选择绘制方式
        DrawType drawType = config.getDrawType();
        if (drawType == DrawType.LINE && points.size() == 2) {
            // 直线绘制
            Offset startPoint = points.get(0);
            Offset endPoint = points.get(1);

            PointF startScreen = convertRelativeToScreen(startPoint.getX(), startPoint.getY(), scale);
            PointF endScreen = convertRelativeToScreen(endPoint.getX(), endPoint.getY(), scale);

            canvas.drawLine(startScreen.x, startScreen.y, endScreen.x, endScreen.y, paint);
        } else {
            // 曲线绘制（默认）
            Path path = new Path();

            PointF firstScreen = convertRelativeToScreen(points.get(0).getX(), points.get(0).getY(), scale);
            path.moveTo(firstScreen.x, firstScreen.y);

            for (int i = 1; i < points.size(); i++) {
                PointF screen = convertRelativeToScreen(points.get(i).getX(), points.get(i).getY(), scale);
                path.lineTo(screen.x, screen.y);
            }

            canvas.drawPath(path, paint);
        }
    }

    /**
     * 将页面相对坐标(0-1)转换为绘制坐标
     * 在 Page.draw() 中调用，返回相对于 pageBounds 的坐标（不包含滚动偏移）
     */
    private PointF convertRelativeToScreen(float relativeX, float relativeY, float scale) {
        // 获取原始页面尺寸
        APage vuPage = documentView.decodeService.getAPage(index);
        if (vuPage == null) {
            return new PointF(0, 0);
        }

        // 将相对坐标转换为页面原始坐标
        float pageX = relativeX * vuPage.getWidth(false);
        float pageY = relativeY * vuPage.getHeight(false);

        // 处理切边
        if (crop && documentView.crop) {
            Rect rect = documentView.getBounds(this);
            if (rect != null) {
                pageX -= rect.left;
                pageY -= rect.top;
            }
        }

        // 应用缩放
        float scaledX = pageX * scale;
        float scaledY = pageY * scale;

        // 转换为相对于 pageBounds 的坐标
        // 在 Page.draw() 中，canvas 已经自动处理了滚动，所以不需要减去 getScrollX/Y
        float resultX = scaledX + bounds.left;
        float resultY = scaledY + bounds.top;

        return new PointF(resultX, resultY);
    }

    public void setSelectionRects(List<RectF> rects) {
        if (selectionRects == null) {
            selectionRects = new ArrayList<>();
        } else {
            selectionRects.clear();
        }

        if (rects != null) {
            selectionRects.addAll(rects);
        }
    }

    public void clearTextSelection() {
        if (selectionRects != null) {
            selectionRects.clear();
        }
    }

    /**
     * 将屏幕坐标转换为页面原始坐标
     * 参考tryHyperlink中的坐标转换逻辑
     */
    public PointF screenToPagePoint(float screenX, float screenY) {
        // 获取缩放因子
        float scale = documentView.calculateScale(this);

        // 将屏幕坐标转换为相对于页面bounds的坐标，并除以缩放因子
        float x = (screenX - bounds.left) / scale;
        float y = (screenY - bounds.top) / scale;

        // 处理切边
        if (crop && documentView.crop) {
            Rect rect = documentView.getBounds(this);
            if (rect != null) {
                x += rect.left;
                y += rect.top;
            }
        }

        return new PointF(x, y);
    }

    /**
     * 将屏幕坐标转换为页面相对坐标（0-1范围）
     * 相对于原始页面尺寸
     */
    public PointF screenToPageRelativePoint(float screenX, float screenY) {
        // 首先获取页面原始坐标
        PointF pagePoint = screenToPagePoint(screenX, screenY);

        // 获取原始页面尺寸
        APage vuPage = documentView.decodeService.getAPage(index);
        if (vuPage == null) {
            return new PointF(0, 0);
        }

        // 转换为相对坐标（0-1）
        float relativeX = pagePoint.x / vuPage.getWidth(false);
        float relativeY = pagePoint.y / vuPage.getHeight(false);

        return new PointF(relativeX, relativeY);
    }

    private void drawTextSelection(Canvas canvas) {
        if (selectionRects == null || selectionRects.isEmpty() || selectionPaint == null) {
            return;
        }

        for (RectF rect : selectionRects) {
            canvas.drawRect(rect, selectionPaint);
        }
    }

    @Override
    public String toString() {
        return "Page{" +
                "index=" + index +
                ", bounds=" + bounds +
                ", node=" + node +
                ", aspectRatio=" + aspectRatio +
                '}';
    }
}
