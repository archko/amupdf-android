package org.vudroid.pdfdroid.codec;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Quad;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.IntentFile;
import cn.archko.pdf.core.decode.MupdfDocument;
import cn.archko.pdf.core.entity.ReflowBean;
import cn.archko.pdf.core.utils.Utils;

public class PdfDocument implements CodecDocument {

    private Document document;

    public void setDocument(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    public CodecPage getPage(int pageNumber) {
        return PdfPage.createPage(document, pageNumber);
    }

    public int getPageCount() {
        return document.countPages();
    }

    public static PdfDocument openDocument(String fname, String pwd) {
        PdfDocument pdfDocument = new PdfDocument();
        Document core = null;
        System.out.println("Trying to open " + fname);
        try {
            core = Document.openDocument(fname);
            if (IntentFile.INSTANCE.isReflowable(fname)) {
                int w = Utils.getScreenWidthPixelWithOrientation(App.Companion.getInstance());
                int h = Utils.getScreenHeightPixelWithOrientation(App.Companion.getInstance());
                core.layout(w, h, 32);
            }
            pdfDocument.setDocument(core);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return pdfDocument;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (null != document) {
            document.destroy();
        }
    }

    @Override
    public List<OutlineLink> loadOutline() {
        Outline[] outlines = document.loadOutline();
        List<OutlineLink> links = new ArrayList<>();
        if (outlines != null) {
            downOutline(document, outlines, links);
        }
        return links;
    }

    @Override
    public List<ReflowBean> decodeReflowText(int index) {
        List<ReflowBean> beans = MupdfDocument.Companion.decodeReflowText(index, document);
        return beans;
    }

    @Override
    public Object[] search(String text, int pageNum) {
        CodecPage page = getPage(pageNum);
        Object[] quads = ((PdfPage) page).page.search(text);
        if (quads == null || quads.length == 0) {
            return null;
        }
        return quads;
    }

    public static void downOutline(Document core, Outline[] outlines, List<OutlineLink> links) {
        if (null != outlines) {
            for (Outline outline : outlines) {
                int page = core.pageNumberFromLocation(core.resolveLink(outline));
                OutlineLink link = new OutlineLink(outline.title, page, 0);
                if (outline.down != null) {
                    Outline[] child = outline.down;
                    downOutline(core, child, links);
                }
                links.add(link);
            }
        }
    }
}
