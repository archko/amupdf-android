package org.vudroid.djvudroid.codec;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.OutlineLink;
import org.vudroid.core.codec.PageTextBox;
import org.vudroid.core.codec.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.archko.pdf.core.entity.ReflowBean;

public class DjvuDocument implements CodecDocument {
    private long contextHandle;
    private long documentHandle;
    private List<OutlineLink> docOutline;

    private DjvuDocument(long contextHandle, long documentHandle, Object waitObject) {
        this.contextHandle = contextHandle;
        this.documentHandle = documentHandle;
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
        return page.getReflowBean();
    }

    @Override
    public List<SearchResult> search(String pattern, int pageNum) {
        List<SearchResult> searchResults = new ArrayList<>();
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            final List<PageTextBox> results = DjvuPage.getPageText(documentHandle,
                    pageNum,
                    contextHandle,
                    pattern.toLowerCase(Locale.ROOT));
            if (results == null || results.isEmpty()) {
                continue;
            }
            for (PageTextBox textBox : results) {
                textBox.page = i;
            }
            StringBuilder sb = new StringBuilder();
            List<ReflowBean> reflowBeans = decodeReflowText(i);
            if (reflowBeans != null && !reflowBeans.isEmpty()) {
                for (ReflowBean bean : reflowBeans) {
                    sb.append(bean.getData()).append(" ");
                }
            }
            searchResults.add(new SearchResult(i, results, sb.toString()));
        }

        return searchResults;
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
