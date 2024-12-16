package org.vudroid.core.codec;

import android.graphics.RectF;

import androidx.annotation.Nullable;

public class PageTextBox extends RectF {
    public PageTextBox() {
    }

    public PageTextBox(float left, float top, float right, float bottom) {
        super(left, top, right, bottom);
    }

    public PageTextBox(@Nullable RectF r) {
        super(r);
    }

    public String text;

    @Override
    public String toString() {
        return "PageTextBox(" + left + ", " + top + ", " + right + ", " + bottom + ": " + text + ")";
    }
}
