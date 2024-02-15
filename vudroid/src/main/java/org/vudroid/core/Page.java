package org.vudroid.core;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

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

        //canvas.drawText("Page " + (index + 1), bounds.centerX(), bounds.centerY(), textPaint);
        node.draw(canvas);
        canvas.drawLine(bounds.left, bounds.top, bounds.right, bounds.top, strokePaint);
        canvas.drawLine(bounds.left, bounds.bottom, bounds.right, bounds.bottom, strokePaint);
    }

    private Paint strokePaint() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        return strokePaint;
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
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            documentView.invalidatePageSizes();
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
