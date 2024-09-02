package org.vudroid.djvudroid.codec;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.entity.ReflowBean;

public class DjvuDocument implements CodecDocument {
    private long contextHandle;
    private long documentHandle;
    private final Object waitObject;
    private List<OutlineLink> docOutline;

    private DjvuDocument(long contextHandle, long documentHandle, Object waitObject) {
        this.contextHandle = contextHandle;
        this.documentHandle = documentHandle;
        this.waitObject = waitObject;
    }

    static DjvuDocument openDocument(String fileName, DjvuContext djvuContext, Object waitObject) {
        return new DjvuDocument(djvuContext.getContextHandle(), open(djvuContext.getContextHandle(), fileName), waitObject);
    }

    private native static long open(long contextHandle, String fileName);

    private native static long getPage(long docHandle, int pageNumber);

    private native static int getPageCount(long docHandle);

    private native static void free(long pageHandle);

    public DjvuPage getPage(int pageNumber) {
        return new DjvuPage(contextHandle, documentHandle, getPage(documentHandle, pageNumber), pageNumber);
    }

    public int getPageCount() {
        return getPageCount(documentHandle);
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (documentHandle == 0) {
            return;
        }
        free(documentHandle);
        documentHandle = 0;
    }

    @Override
    public List<OutlineLink> loadOutline() {
        List<OutlineLink> list = getOutline();
        return list;
    }

    public List<OutlineLink> getOutline() {
        if (docOutline == null) {
            final DjvuOutline ou = new DjvuOutline();
            docOutline = ou.getOutline(documentHandle);
        }
        return docOutline;
    }

    public List<ReflowBean> decodeReflowText(int index) {
        DjvuPage page = getPage(index);
        String rest = page.getPageHTML();
        page.recycle();
        List<ReflowBean> list = new ArrayList<>();
        ReflowBean bean = new ReflowBean(rest, ReflowBean.TYPE_STRING, null);
        list.add(bean);
        return list;
    }

    /*public List<? extends RectF> searchText(final int pageNuber, final String pattern) {
        final List<PageTextBox> list = DjvuPage.getPageText(documentHandle,
                pageNuber,
                context.getContextHandle(),
                pattern.toLowerCase(Locale.ROOT));
        if (LengthUtils.isNotEmpty(list)) {
            CodecPageInfo cpi = getPageInfo(pageNuber);
            for (final PageTextBox ptb : list) {
                DjvuPage.normalizeTextBox(ptb, cpi.width, cpi.height);
            }
        }
        return list;
    }*/
}
