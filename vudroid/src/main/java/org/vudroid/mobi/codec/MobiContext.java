package org.vudroid.mobi.codec;

import android.content.ContentResolver;

import com.archko.reader.mobi.LibMobi;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.pdfdroid.codec.PdfDocument;

import java.io.File;

import cn.archko.pdf.core.common.IntentFile;

public class MobiContext implements CodecContext {

    public CodecDocument openDocument(String fileName) {
        String path = "";
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

        return PdfDocument.openDocument(path, "");
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    public void recycle() {
    }
}
