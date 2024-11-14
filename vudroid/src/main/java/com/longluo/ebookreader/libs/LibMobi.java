package com.longluo.ebookreader.libs;

import java.io.File;

/**
 * added for support mobi, azw, azw3, azw4.
 * <p>
 * by longluo
 */
public class LibMobi {

    static {
        System.loadLibrary("mobi");
    }

    public static native int convertToEpub(String input, String output);

    public static void openMobiAzwBook(Activity activity, File file) {
        String path = file.getAbsolutePath();
        String folderPath = path.substring(0, path.lastIndexOf("/"));

        String hashCodeStr = path.hashCode() + "";
        String convertFilePath = folderPath + File.separator + hashCodeStr + ".epub";
        Log.d("", "openMobiAzwBook: file=" + path + ", folder=" + folderPath
                + ",convertFilePath=" + convertFilePath);
        File convertFile = new File(convertFilePath);
        if (!convertFile.exists()) {
            LibMobi.convertToEpub(path, new File(folderPath, hashCodeStr).getPath());
        }
        File firstConvertFile = new File(folderPath + File.separator + hashCodeStr + hashCodeStr + ".epub");
        if (firstConvertFile.exists()) {
            firstConvertFile.renameTo(new File(convertFilePath));
        }
    }
}
