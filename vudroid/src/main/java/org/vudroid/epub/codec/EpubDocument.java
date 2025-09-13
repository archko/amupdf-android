package org.vudroid.epub.codec;

import com.artifex.mupdf.fitz.Document;
import com.tencent.mmkv.MMKV;

import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.utils.Utils;

public class EpubDocument extends PdfDocument {

    public static float getDefFontSize() {
        float fontSize = (7.2f * Utils.getDensityDpi(App.Companion.getInstance()) / 72);
        return fontSize;
    }

    public static float getFontSize() {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        var fs = mmkv.decodeFloat("font", getDefFontSize());
        if (fs > 80) {
            fs = 80f;
        }
        return fs;
    }

    public static void setFontSize(float size) {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        Logcat.d("setFontSize:$size");
        mmkv.encode("font", size);
    }

    public static EpubDocument openDocument(String fname, String pwd) {
        EpubDocument epubDocument = new EpubDocument();
        Document document = null;
        try {
            document = Document.openDocument(fname);
            int w = Utils.getScreenWidthPixelWithOrientation(App.Companion.getInstance());
            int h = Utils.getScreenHeightPixelWithOrientation(App.Companion.getInstance());
            float fontSize = getFontSize();
            System.out.printf("font:%s, open:%s%n", fontSize, fname);
            document.layout(w, h, fontSize);
            epubDocument.setDocument(document);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return epubDocument;
    }
}
