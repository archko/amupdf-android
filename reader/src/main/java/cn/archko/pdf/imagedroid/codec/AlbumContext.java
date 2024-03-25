package cn.archko.pdf.imagedroid.codec;

import android.content.ContentResolver;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;

public class AlbumContext implements CodecContext {

    public CodecDocument openDocument(String fileName) {
        return AlbumDocument.openDocument(fileName);
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    public void recycle() {
    }
}
