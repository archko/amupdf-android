package com.artifex.solib;

import android.graphics.RectF;

public class SOLinkData {

    public SOLinkData(int page, RectF box) {
        this.page = page;
        this.box = box;
    }

    public int page;
    public RectF box;
}
