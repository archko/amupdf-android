package org.vudroid.core.codec;

import java.util.List;

import cn.archko.pdf.core.entity.ReflowBean;

public interface CodecDocument {
    CodecPage getPage(int pageNumber);

    int getPageCount();

    void recycle();

    List<OutlineLink> loadOutline();

    List<ReflowBean> decodeReflowText(int index);

    Object[] search(String text, int pageNum);
}
