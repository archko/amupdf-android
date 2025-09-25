package org.vudroid.epub.codec;

import android.text.TextUtils;

import com.artifex.mupdf.fitz.Context;
import com.artifex.mupdf.fitz.Document;
import com.tencent.mmkv.MMKV;

import org.vudroid.pdfdroid.codec.PdfDocument;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.utils.Utils;

public class EpubDocument extends PdfDocument {

    public static float getDefFontSize() {
        float fontSize = (9f * Utils.getDensityDpi(App.Companion.getInstance()) / 72);
        return fontSize;
    }

    public static float getFontSize(String name) {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        var fs = mmkv.decodeFloat("font_" + name.hashCode(), getDefFontSize());
        if (fs > 90) {
            fs = 90f;
        }
        return fs;
    }

    public static void setFontSize(String name, float size) {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        Logcat.d("setFontSize:" + size);
        mmkv.encode("font_" + name.hashCode(), size);
    }

    //"/sdcard/fonts/simsun.ttf"
    public static String getFontFace() {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        var fs = mmkv.decodeString("font_face", "");
        return fs;
    }

    public static void setFontFace(String name) {
        MMKV mmkv = MMKV.mmkvWithID("epub");
        Logcat.d("setFontFace:" + name);
        mmkv.encode("font_face", name);
    }

    public static EpubDocument openDocument(String fname, String pwd) {
        // 生成并应用自定义字体CSS
        String css = FontCSSGenerator.INSTANCE.generateFontCSS(getFontFace(), "10px");
        if (!TextUtils.isEmpty(css)) {
            System.out.println("应用自定义CSS: " + css);
        }
        Context.setUserCSS(css);
        EpubDocument epubDocument = new EpubDocument();
        Document document = null;
        try {
            document = Document.openDocument(fname);
            int w = Utils.getScreenWidthPixelWithOrientation(App.Companion.getInstance());
            int h = Utils.getScreenHeightPixelWithOrientation(App.Companion.getInstance());
            float fontSize = getFontSize(fname);
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
