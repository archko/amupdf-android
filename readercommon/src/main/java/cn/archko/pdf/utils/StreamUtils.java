package cn.archko.pdf.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class StreamUtils {

    private static final Object TAG = "StreamUtils";

    /**
     * 将字符串保存到指定的文件中
     */
    public static void saveStringToFile(String text, String filePath) {
        File file = new File(filePath);
        saveStringToFile(text, file);
    }

    /**
     * 将字符串保存到指定的文件中
     *
     * @param text
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean saveStringToFile(String text, File file) {
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        return saveStreamToFile(in, file);
    }

    /**
     * 将输入流保存到指定的文件中
     */
    public static synchronized boolean saveStreamToFile(InputStream in, String filePath) {
        File file = new File(filePath);
        return saveStreamToFile(in, file);
    }

    /**
     * 将输入流保存到指定的文件中
     */
    public static synchronized boolean saveStreamToFile(InputStream in, File file) {
        FileOutputStream fos = null;
        try {

            if (file.exists()) {
                file.delete();
            } else {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }

            fos = new FileOutputStream(file);
            copyStream(in, fos);
            return true;
        } catch (Exception e) {
        } finally {
            closeStream(fos);
        }
        return false;
    }

    //-------------------------------------------------------

    /**
     * 从输入流里面读出字节数组
     */
    public static byte[] readByteFromStream(InputStream in) throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();

            byte[] buf = new byte[1024];
            int len = -1;
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toByteArray();
        } finally {
            closeStream(bos);
            closeStream(in);
        }
    }

    public static String readString(Reader reader) {
        BufferedReader bufferedReader = null;
        StringBuilder sb = new StringBuilder();
        try {
            bufferedReader = new BufferedReader(reader);
            String temp = null;
            while ((temp = bufferedReader.readLine()) != null) {
                sb.append(temp);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            closeStream(bufferedReader);
        }
        return sb.toString();
    }

    /**
     * 从文件中读取字符串
     */
    public static String readStringFromFile(String filePath) {
        return readStringFromFile(new File(filePath));
    }

    /**
     * 从文件中读取字符串
     */
    public static String readStringFromFile(File file) {
        if (file != null && file.exists() && file.isFile() && file.length() > 0) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                return readStringFromInputStream(fis);
            } catch (Exception e) {
            } finally {
                closeStream(fis);
            }
        }
        return "";
    }

    /**
     * 从输入流中读取字符串（以 UTF-8 编码）
     * 本方法不会关闭InputStream
     */
    public static final String readStringFromInputStream(InputStream in) {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[512];
            int len;
            while ((len = in.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
        } finally {
            closeStream(bos);
        }
        return "";
    }

    /**
     * 关闭流
     *
     * @param closeable 可关闭的对象
     */
    public static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 读取输入流，并将其数据输出到输出流中。
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(in);
        BufferedOutputStream bout = new BufferedOutputStream(out);

        byte[] buffer = new byte[4096];

        while (true) {
            int doneLength = bin.read(buffer);
            if (doneLength == -1) {
                break;
            }
            bout.write(buffer, 0, doneLength);
        }
        bout.flush();
    }

    public static void copyStringToFile(String text, String filePath) throws IOException {
        FileUtils.copy(new ByteArrayInputStream(text.getBytes("UTF-8")), new FileOutputStream(filePath));
    }

}
