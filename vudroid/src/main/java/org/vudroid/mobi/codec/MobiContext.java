package org.vudroid.mobi.codec;

import android.content.ContentResolver;

import com.archko.reader.mobi.LibMobi;
import com.artifex.mupdf.fitz.Context;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.pdfdroid.codec.PdfDocument;

import java.io.File;

import cn.archko.pdf.core.common.IntentFile;

public class MobiContext implements CodecContext {

    public CodecDocument openDocument(String fileName) {
        String path = fileName;
        if (IntentFile.INSTANCE.isMobi(fileName)) {
            File result = LibMobi.convertMobiToEpub(new File(fileName));
            if (null == result || !result.exists()) {
                return null;
            }
            path = result.getAbsolutePath();
        } else if (IntentFile.INSTANCE.isDocx(fileName)) {
            File result = LibMobi.convertDocxToHtml(new File(fileName));
            if (null == result || !result.exists()) {
                return null;
            }
            path = result.getAbsolutePath();
        }

        //String css="@font-face {font-family:\"msyh\" ,\"simsun\",\"menlo\"; font-weight:normal; font-style:normal;}";
        //String css="@font-face {font-family:\"DroidSansMono\" ,\"simsun\",\"menlo\"; font-weight:normal; font-style:normal;}";
        //String css="* {line-height:2f,font-size:1.8em, font-family: 'DroidSansMono', 'NotoSans' ,'MiSansVF', 'menlo' ! important;}";
        //Context.setUserCSS(css);

        return PdfDocument.openDocument(path, "");
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    public void recycle() {
    }
}
