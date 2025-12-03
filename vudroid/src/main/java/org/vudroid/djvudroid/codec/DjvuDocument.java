package org.vudroid.djvudroid.codec;

import com.archko.reader.image.DjvuLoader;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.OutlineLink;
import org.vudroid.core.codec.PageTextBox;
import org.vudroid.core.codec.SearchResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.archko.pdf.core.entity.ReflowBean;

public class DjvuDocument implements CodecDocument {
    private DjvuLoader djvuLoader;
    private List<OutlineLink> docOutline;

    private DjvuDocument(DjvuLoader djvuLoader) {
        this.djvuLoader = djvuLoader;
    }

    static DjvuDocument openDocument(String fileName, DjvuContext djvuContext) {
        DjvuLoader loader = new DjvuLoader();
        loader.openDjvu(new File(fileName).getAbsolutePath());
        return new DjvuDocument(loader);
    }

    public DjvuPage getPage(int pageNumber) {
        return new DjvuPage(djvuLoader, pageNumber);
    }

    public int getPageCount() {
        return djvuLoader.getDjvuInfo().getPages();
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (djvuLoader != null) {
            djvuLoader.close();
            djvuLoader = null;
        }
    }

    @Override
    public List<OutlineLink> loadOutline() {
        List<OutlineLink> list = getOutline();
        return list;
    }

    public List<OutlineLink> getOutline() {
        if (docOutline == null) {
            final DjvuOutline ou = new DjvuOutline();
            docOutline = ou.getOutline(djvuLoader);
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
            final List<PageTextBox> results = DjvuPage.getPageTextSync(djvuLoader, pageNum, pattern.toLowerCase(Locale.ROOT));
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
}
