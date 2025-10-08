package org.vudroid.epub.codec;

import android.content.ContentResolver;
import android.util.Log;

import com.archko.reader.mobi.LibMobi;

import org.vudroid.core.codec.CodecContext;
import org.vudroid.core.codec.CodecDocument;

import java.io.File;

import cn.archko.pdf.core.common.IntentFile;

public class EpubContext implements CodecContext {

    public CodecDocument openDocument(String fileName) {
        String path = fileName;
        if (IntentFile.INSTANCE.isAzw(fileName)) {
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
        Log.d("TAG", "open epub:" + path);

        //String css="@font-face {font-family:\"msyh\" ,\"simsun\",\"menlo\"; font-weight:normal; font-style:normal;}";
        //String css="@font-face {font-family:\"DroidSansMono\" ,\"simsun\",\"menlo\"; font-weight:normal; font-style:normal;}";
        //String css="* {line-height:2f,font-size:1.8em, font-family: 'DroidSansMono', 'NotoSans' ,'MiSansVF', 'menlo' ! important;}";
        //Context.setUserCSS(css);

        return EpubDocument.openDocument(path, "");
    }

    public void setContentResolver(ContentResolver contentResolver) {
    }

    public void recycle() {
    }
}
