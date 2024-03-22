package org.vudroid.core.codec;

import com.artifex.mupdf.fitz.Outline;

public interface CodecDocument {
    CodecPage getPage(int pageNumber);

    int getPageCount();

    void recycle();

    Outline[] loadOutline();
}
