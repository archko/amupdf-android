package com.baidu.ai.edge.core.util;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/* loaded from: easyedge-sdk.jar:com/baidu/ai/edge/core/util/FileUtil.class */
public class FileUtil {
    public static String readFile(String str) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(str);
        int available = fileInputStream.available();
        byte[] bArr = new byte[available];
        int read = fileInputStream.read(bArr);
        fileInputStream.close();
        if (read == available) {
            return new String(bArr);
        }
        throw new IOException("readFile realSize is not equal to size: " + read + " : " + available);
    }

    public static String readAssetFileUtf8String(AssetManager assetManager, String str) throws IOException {
        return new String(readAssetFileContent(assetManager, str), Charset.forName("UTF-8"));
    }

    public static byte[] readAssetFileContent(AssetManager assetManager, String str) throws IOException {
        Log.i("FileUtil", "try to read asset file: " + str);
        InputStream open = assetManager.open(str);
        int available = open.available();
        byte[] bArr = new byte[available];
        int read = open.read(bArr);
        open.close();
        if (read == available) {
            return bArr;
        }
        throw new IOException("realSize is not equal to size: " + read + " : " + available);
    }

    public static boolean hasLf(String str) {
        new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + str).exists();
        return true;
    }

    public static boolean makeDir(String str) {
        File file = new File(str);
        if (file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    public static String readAssetsFileUTF8StringIfExists(AssetManager assetManager, String str) {
        Log.i("FileUtil", "try to read asset file: " + str);
        InputStream is = null;
        try {
            is = assetManager.open(str);
            int available = is.available();
            byte[] bArr = new byte[available];
            int read = is.read(bArr);
            if (read == available) {
                String result = new String(bArr, Charset.forName("UTF-8"));
                is.close();
                return result;
            }
            throw new IOException("realSize is not equal to size: " + read + " : " + available);
        } catch (IOException e) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException unused) {
                    unused.printStackTrace();
                }
            }
            return null;
        }
    }

    public static String readFileIfExists(String str) {
        File file = new File(str);
        if (!file.exists()) {
            return null;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int available = fis.available();
            byte[] bArr = new byte[available];
            int read = fis.read(bArr);
            if (read == available) {
                String result = new String(bArr);
                fis.close();
                return result;
            }
            throw new IOException("readFile realSize is not equal to size: " + read + " : " + available);
        } catch (IOException e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException unused) {
                    unused.printStackTrace();
                }
            }
            return null;
        }
    }
}
