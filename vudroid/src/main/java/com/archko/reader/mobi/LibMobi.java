package com.archko.reader.mobi;

import android.util.Log;

import java.io.File;
import java.util.List;

import cn.archko.pdf.core.App;
import cn.archko.pdf.core.common.Logcat;
import cn.archko.pdf.core.utils.FileUtils;

/**
 * added for support doc, docx files.
 */
public class LibMobi {

    static {
        System.loadLibrary("mobi");
    }

    public static native int convertToEpub(String input, String output);

    public static File convertMobiToEpub(File file) {
        String input = file.getAbsolutePath();
        int hashCode = (input + file.length() + file.lastModified()).hashCode();
        File outputFile = FileUtils.getDiskCacheDir(App.Companion.getInstance(), hashCode + ".epub");
        Logcat.d(String.format("convertMobiToEpub: file=%s, convertFilePath=%s",
                input, outputFile.getAbsoluteFile()));

        int res = -1;
        if (!outputFile.exists()) {
            try {
                res = LibMobi.convertToEpub(input, outputFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e("", e.getMessage());
            }
        } else {
            res = 0;
        }

        if (res != 0) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        return outputFile;
    }

    public static int convertMobiToEpubBatch(List<String> paths) {
        int count = 0;
        for (String path : paths) {
            File file = new File(path);
            String input = file.getAbsolutePath();
            String name = FileUtils.getNameWithoutExt(input);
            String folderPath = input.substring(0, input.lastIndexOf("/"));
            File outputFile = new File(folderPath + File.separator + name + ".epub");
            Logcat.d(String.format("convertMobiToEpubBatch: file=%s, convertFilePath=%s",
                    input, outputFile.getAbsoluteFile()));

            int res = -1;
            if (!outputFile.exists()) {
                try {
                    res = LibMobi.convertToEpub(input, outputFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.e("", e.getMessage());
                }
            } else {
                res = 0;
            }

            if (res != 0) {
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            } else {
                count++;
            }
        }
        return count;
    }
}
