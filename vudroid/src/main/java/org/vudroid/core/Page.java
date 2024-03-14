package org.vudroid.core;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import org.vudroid.R;
import org.vudroid.core.link.Hyperlink;

import java.util.List;

public class Page {
    final int index;
    RectF bounds;
    private PageTreeNode node;
    private DocumentView documentView;
    public List<Hyperlink> links;
    private final TextPaint textPaint = textPaint();
    private final Paint fillPaint = fillPaint();
    private final Paint strokePaint = strokePaint();
    private final Paint linkPaint = linkPaint();
    public static final int ZOOM_THRESHOLD = 2;

    Page(DocumentView documentView, int index) {
        this.documentView = documentView;
        this.index = index;
        node = new PageTreeNode(documentView, new RectF(0, 0, 1, 1), this, ZOOM_THRESHOLD, null);
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
        canvas.drawRect(bounds, fillPaint);

        canvas.drawText("Page " + (index + 1), bounds.centerX(), bounds.centerY(), textPaint);
        node.draw(canvas);
        //canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint);
        canvas.drawLine(bounds.left, bounds.bottom, bounds.right/5, bounds.bottom, strokePaint);
        drawPageLinks(canvas);
    }

    private Paint strokePaint() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);
        return strokePaint;
    }

    private Paint linkPaint() {
        final Paint linkPaint = new Paint();
        linkPaint.setColor(Color.parseColor("#80FFFF00"));
        linkPaint.setStyle(Paint.Style.FILL);
        return linkPaint;
    }

    private Paint fillPaint() {
        final Paint fillPaint = new Paint();
        //fillPaint.setColor(Color.GRAY);
        fillPaint.setColor(Color.WHITE);    //scroll back show white bg
        fillPaint.setStyle(Paint.Style.FILL);
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

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            boolean changed = aspectRatio - this.aspectRatio > 0.01;
            this.aspectRatio = aspectRatio;
            if (changed) {
                documentView.invalidatePageSizes();
            }
        }
    }

    public boolean isVisible() {
        return RectF.intersects(documentView.getViewRect(), bounds);
    }

    public void setAspectRatio(int width, int height) {
        setAspectRatio(width * 1.0f / height);
    }

    void setBounds(RectF pageBounds) {
        bounds = pageBounds;
        node.invalidateNodeBounds();
    }

    public void updateVisibility() {
        node.updateVisibility();
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
