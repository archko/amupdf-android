package org.vudroid.core;

import android.graphics.RectF;

/**
 * @author: archko 2024/9/2 :14:43
 */
public class DecodeTask {
    public static final int TYPE_PAGE = 0;
    public static final int TYPE_NODE = 1;
    public final String decodeKey;
    public final PageTreeNode node;
    public final int pageNumber;
    public final int type;
    public final float zoom;
    public final DecodeService.DecodeCallback decodeCallback;
    public final RectF pageSliceBounds;
    public boolean crop = true;
    public int dpi = 0;
    public final float fontSize;

    public DecodeTask(PageTreeNode node, boolean crop, int pageNumber, DecodeService.DecodeCallback decodeCallback, float zoom, String decodeKey, RectF pageSliceBounds, int dpi, float fontSize) {
        this.node = node;
        this.crop = crop;
        this.type = node == null ? TYPE_PAGE : TYPE_NODE;
        this.pageNumber = pageNumber;
        this.decodeCallback = decodeCallback;
        this.zoom = zoom;
        this.decodeKey = decodeKey;
        this.pageSliceBounds = pageSliceBounds;
        this.dpi = dpi;
        this.fontSize = fontSize;
    }

    @Override
    public String toString() {
        return "Task{" +
                "page=" + pageNumber +
                ", zoom=" + zoom +
                ", bounds=" + pageSliceBounds +
                ", Key=" + decodeKey +
                '}';
    }
}