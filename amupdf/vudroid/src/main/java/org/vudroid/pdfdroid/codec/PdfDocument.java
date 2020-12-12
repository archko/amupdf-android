package org.vudroid.pdfdroid.codec;

import com.artifex.mupdf.fitz.Document;

import org.vudroid.core.codec.CodecDocument;
import org.vudroid.core.codec.CodecPage;

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
}
