package org.vudroid.pdfdroid.codec;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Outline;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

import cn.archko.pdf.core.decode.MupdfDocument;
import cn.archko.pdf.core.entity.ReflowBean;

public class PdfDocument implements CodecDocument {

    private Document core;

    public void setCore(Document core) {
        this.core = core;
    }

    public Document getCore() {
        return core;
    }

    public CodecPage getPage(int pageNumber) {
        return PdfPage.createPage(core, pageNumber);
    }

    public int getPageCount() {
        return core.countPages();
    }

    public static PdfDocument openDocument(String fname, String pwd) {
        //return new PdfDocument(open(FITZMEMORY, fname, pwd));
        PdfDocument document = new PdfDocument();
        Document core = null;
        System.out.println("Trying to open " + fname);
        try {
            core = Document.openDocument(fname);
            if (fname.endsWith("epub") || fname.endsWith("mobi")) {
                core.layout(MupdfDocument.LAYOUTW, MupdfDocument.LAYOUTH, MupdfDocument.LAYOUTEM);
            }
            document.setCore(core);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return document;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }

    public synchronized void recycle() {
        if (null != core) {
            core.destroy();
        }
    }

    @Override
    public List<OutlineLink> loadOutline() {
        Outline[] outlines = core.loadOutline();
        List<OutlineLink> links = new ArrayList<>();
        if (outlines != null) {
            downOutline(core, outlines, links);
        }
        return links;
    }

    @Override
    public List<ReflowBean> decodeReflowText(int index) {
        List<ReflowBean> beans = MupdfDocument.Companion.decodeReflowText(index, core);
        return beans;
    }

    public static void downOutline(Document core, Outline[] outlines, List<OutlineLink> links) {
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
