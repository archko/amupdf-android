package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;

import org.vudroid.R;
import org.vudroid.core.codec.PageTextBox;
import org.vudroid.core.codec.SearchResult;

import java.lang.ref.SoftReference;
import java.util.List;

import cn.archko.pdf.core.cache.BitmapCache;
import cn.archko.pdf.core.common.Logcat;
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

    Page(DocumentView documentView, int index, boolean crop, ColorFilter filter) {
        this.documentView = documentView;
        this.index = index;
        this.crop = crop;
        this.filter = filter;
        fillPaint = fillPaint();
        node = new PageTreeNode(documentView, new RectF(0, 0, 1, 1), this, ZOOM_THRESHOLD, null, filter);
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
        paint.setColor(Color.parseColor("#80FF9800"));
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
    }

    public void updateVisibility() {
        if (null == bounds) { //tts后台滚动的时候会出现
            return;
        }
        SearchResult result = documentView.getSearchResult(index);
        if (null != result) {
            searchBoxs = result.boxes;
        } else {
            searchBoxs = null;
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
        if (searchBoxs != null && !searchBoxs.isEmpty()) {
            for (PageTextBox rectF : searchBoxs) {
                final RectF rect = getPageRegion(bounds, rectF);
                Logcat.d(String.format("result:%s, rect:%s, %s", index, rect, rectF));
                if (rect != null) {
                    canvas.drawRect(rect, searchPaint);
                }
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
