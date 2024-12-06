package org.vudroid.epub.codec;

import com.artifex.mupdf.fitz.Document;

import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.utils.Utils;

public class EpubDocument extends PdfDocument {

    public static EpubDocument openDocument(String fname, String pwd) {
        EpubDocument epubDocument = new EpubDocument();
        Document document = null;
        System.out.println("Trying to open " + fname);
        try {
            document = Document.openDocument(fname);
            int w = Utils.getScreenWidthPixelWithOrientation(App.Companion.getInstance());
            int h = Utils.getScreenHeightPixelWithOrientation(App.Companion.getInstance());
            document.layout(w, h, 32);
            epubDocument.setDocument(document);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return epubDocument;
    }
}
