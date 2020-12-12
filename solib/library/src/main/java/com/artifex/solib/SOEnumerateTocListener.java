package com.artifex.solib;

import android.graphics.RectF;

public interface SOEnumerateTocListener
{
    void nextTocEntry(int handle, int parentHandle, String label, String url);
}
