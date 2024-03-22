package org.vudroid.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.lang.ref.SoftReference;
import java.util.Arrays;

import cn.archko.pdf.common.BitmapCache;
import cn.archko.pdf.common.BitmapPool;

public class PageTreeNode {
    private static final int SLICE_SIZE = 256 * 256 * 4;
    private Bitmap bitmap;
    private SoftReference<Bitmap> bitmapWeakReference;
    private boolean decodingNow;
    private final RectF pageSliceBounds;
    final Page page;
    private PageTreeNode[] children;
    private final int treeNodeDepthLevel;
    private Matrix matrix = new Matrix();
    private final Paint bitmapPaint = new Paint();
    private DocumentView documentView;
    private boolean invalidateFlag;
    private Rect targetRect;
    private RectF targetRectF;
    private final Paint strokePaint = strokePaint();

    private Paint strokePaint() {
        final Paint strokePaint = new Paint();
        strokePaint.setColor(Color.GREEN);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2);
        return strokePaint;
    }

    PageTreeNode(DocumentView documentView, RectF localPageSliceBounds, Page page, int treeNodeDepthLevel, PageTreeNode parent) {
        this.documentView = documentView;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);
        this.page = page;
        this.treeNodeDepthLevel = treeNodeDepthLevel;
    }

    public void updateVisibility() {
        invalidateChildren();
        if (children != null) {
            for (PageTreeNode child : children) {
                child.updateVisibility();
            }
        }
        if (isVisible()) {
            if (!thresholdHit()) {
                if (getBitmap() != null && !invalidateFlag) {
                    restoreBitmapReference();
                } else {
                    decodePageTreeNode();
                }
            }
        }
        if (!isVisibleAndNotHiddenByChildren()) {
            stopDecodingThisNode();
            setBitmap(null);
        }
    }

    public void invalidate() {
        invalidateChildren();
        invalidateRecursive();
        updateVisibility();
    }

    private void invalidateRecursive() {
        invalidateFlag = true;
        if (children != null) {
            for (PageTreeNode child : children) {
                child.invalidateRecursive();
            }
        }
        stopDecodingThisNode();
    }

    void invalidateNodeBounds() {
        targetRect = null;
        targetRectF = null;
        if (children != null) {
            for (PageTreeNode child : children) {
                //System.out.println(String.format("level:%s, page:%s, slice:%s, target:%s",treeNodeDepthLevel,page.index, pageSliceBounds, getTargetRect()));
                child.invalidateNodeBounds();
            }
        }
    }

    void draw(Canvas canvas) {
        if (!isVisible()) {
            return;
        }
        Bitmap bmp = getBitmap();
        if (bmp != null && !bmp.isRecycled()) {
            //System.out.println(String.format("level:%s, page:%s, width:%s, %s", treeNodeDepthLevel, page.index, getBitmap().getWidth(), getBitmap().getHeight()));
            canvas.drawBitmap(bmp, new Rect(0, 0, getBitmap().getWidth(), getBitmap().getHeight()), getTargetRect(), bitmapPaint);
            //canvas.drawRect(getTargetRect(), strokePaint);
        }
        if (children == null) {
            return;
        }

        //System.out.println(String.format("level:%s, page:%s, %s, target:%s,%s", treeNodeDepthLevel, page.index, pageSliceBounds, getTargetRect(), documentView.getViewRect()));
        for (PageTreeNode child : children) {
            child.draw(canvas);
        }
    }

    private boolean isVisible() {
        //if (documentView.zoomModel.getZoom() >= 1.5) {
        return RectF.intersects(documentView.getViewRect(), getTargetRectF());
        //} else {
        //    return RectF.intersects(documentView.getViewRectForPage(), getTargetRectF());
        //}
    }

    private RectF getTargetRectF() {
        if (targetRectF == null) {
            targetRectF = new RectF(getTargetRect());
        }
        return targetRectF;
    }

    private void invalidateChildren() {
        boolean isThresholdHit = thresholdHit();
        boolean isVisible = isVisible();
        if (isThresholdHit && children == null && isVisible) {
            final int newThreshold = treeNodeDepthLevel * 2;
            children = new PageTreeNode[]
                    {
                            new PageTreeNode(documentView, new RectF(0, 0, 0.5f, 0.5f), page, newThreshold, this),
                            new PageTreeNode(documentView, new RectF(0.5f, 0, 1.0f, 0.5f), page, newThreshold, this),
                            new PageTreeNode(documentView, new RectF(0, 0.5f, 0.5f, 1.0f), page, newThreshold, this),
                            new PageTreeNode(documentView, new RectF(0.5f, 0.5f, 1.0f, 1.0f), page, newThreshold, this)
                    };
        }
        if (!isThresholdHit && getBitmap() != null || !isVisible) {
            recycleChildren();
        }
    }

    private boolean thresholdHit() {
        float zoom = documentView.zoomModel.getZoom();
        int mainWidth = documentView.getWidth();
        float height = page.getPageHeight(mainWidth, zoom);
        return (mainWidth * zoom * height) / (treeNodeDepthLevel * treeNodeDepthLevel) > SLICE_SIZE;
    }

    public Bitmap getBitmap() {
        Bitmap bitmap = bitmapWeakReference != null ? bitmapWeakReference.get() : null;
        //if (null == bitmap) {
        //    bitmap = BitmapCache.getInstance().getBitmap(getCacheKey());
        //}
        return bitmap;
    }

    private String getCacheKey() {
        return String.format("%s-%s-%s", page.index, pageSliceBounds, page);
    }

    private void restoreBitmapReference() {
        setBitmap(getBitmap());
    }

    private final DecodeService.DecodeCallback decodeCallback = new DecodeService.DecodeCallback() {
        @Override
        public void decodeComplete(Bitmap bitmap, boolean isThumb) {
            //System.out.println(String.format("DecodeService index:%s, bitmap:%s, key:%s", page.index, bitmap == null, getCacheKey()));

            setBitmap(bitmap);
            invalidateFlag = false;
            setDecodingNow(false);
            page.setAspectRatio(documentView.decodeService.getPageWidth(page.index), documentView.decodeService.getPageHeight(page.index));
            invalidateChildren();
        }

        @Override
        public boolean shouldRender(int pageNumber, boolean isFullPage) {
            if (getBitmap() != null) {
                return false;
            }
            boolean isVisible = isVisible();
            if (!isVisible) {
                setBitmap(null);
                setDecodingNow(false);
                invalidateChildren();
            }
            return isVisible;
        }
    };

    private void decodePageTreeNode() {
        if (isDecodingNow()) {
            return;
        }
        setDecodingNow(true);
        documentView.decodeService.decodePage(getCacheKey(),
                this,
                page.index,
                decodeCallback,
                documentView.zoomModel.getZoom(),
                pageSliceBounds,
                page.getKey());
    }

    private RectF evaluatePageSliceBounds(RectF localPageSliceBounds, PageTreeNode parent) {
        if (parent == null) {
            return localPageSliceBounds;
        }
        final Matrix matrix = new Matrix();
        matrix.postScale(parent.pageSliceBounds.width(), parent.pageSliceBounds.height());
        matrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top);
        final RectF sliceBounds = new RectF();
        matrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
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
                BitmapCache.getInstance().remove(getCacheKey());
                BitmapPool.getInstance().release(bitmap);
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
            if (decodingNow) {
                documentView.progressModel.increase();
            } else {
                documentView.progressModel.decrease();
            }
        }
    }

    private Rect getTargetRect() {
        if (targetRect == null) {
            matrix.reset();
            matrix.postScale(page.bounds.width(), page.bounds.height());
            matrix.postTranslate(page.bounds.left, page.bounds.top);
            RectF targetRectF = new RectF();
            matrix.mapRect(targetRectF, pageSliceBounds);
            targetRect = new Rect((int) targetRectF.left, (int) targetRectF.top, (int) targetRectF.right, (int) targetRectF.bottom);
        }
        return targetRect;
    }

    private void stopDecodingThisNode() {
        if (!isDecodingNow()) {
            return;
        }
        documentView.decodeService.stopDecoding(getCacheKey());
        setDecodingNow(false);
    }

    private boolean isHiddenByChildren() {
        if (children == null) {
            return false;
        }
        for (PageTreeNode child : children) {
            if (child.getBitmap() == null) {
                return false;
            }
        }
        return true;
    }

    private void recycleChildren() {
        if (children == null) {
            return;
        }
        for (PageTreeNode child : children) {
            child.recycle();
        }
        if (!childrenContainBitmaps()) {
            children = null;
        }
    }

    private boolean containsBitmaps() {
        return getBitmap() != null || childrenContainBitmaps();
    }

    private boolean childrenContainBitmaps() {
        if (children == null) {
            return false;
        }
        for (PageTreeNode child : children) {
            if (child.containsBitmaps()) {
                return true;
            }
        }
        return false;
    }

    private void recycle() {
        stopDecodingThisNode();
        setBitmap(null);
        if (children != null) {
            for (PageTreeNode child : children) {
                child.recycle();
            }
        }
    }

    private boolean isVisibleAndNotHiddenByChildren() {
        return isVisible() && !isHiddenByChildren();
    }

    @Override
    public String toString() {
        return "PageTreeNode{" +
                "treeNodeDepthLevel=" + treeNodeDepthLevel +
                ", children=" + Arrays.toString(children) +
                ", pageSliceBounds=" + pageSliceBounds +
                ", bitmap=" + bitmap +
                ", bitmapWeakReference=" + bitmapWeakReference +
                ", targetRectF=" + targetRectF +
                ", targetRect=" + targetRect +
                ", matrix=" + matrix +
                '}';
    }
}
