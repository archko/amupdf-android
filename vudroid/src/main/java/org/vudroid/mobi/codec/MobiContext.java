package org.vudroid.mobi.codec;

import android.content.ContentResolver;

import com.archko.reader.mobi.LibMobi;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;
import org.vudroid.pdfdroid.codec.PdfDocument;

import java.io.File;

public class MobiContext implements CodecContext {

    public CodecDocument openDocument(String fileName) {
        File result = LibMobi.convertMobiToEpub(new File(fileName));
        if (null == result || !result.exists()) {
            return null;
        }
        return PdfDocument.openDocument(result.getAbsolutePath(), "");
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    public void recycle() {
    }
}
