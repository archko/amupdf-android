package cn.archko.pdf.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import cn.archko.pdf.App;

public final class FileUtils {

    private static ArrayList<String> mounts = new ArrayList<String>();
    private static ArrayList<String> mountsPR = new ArrayList<String>();
    private static ArrayList<String> aliases = new ArrayList<String>();
    private static ArrayList<String> aliasesPR = new ArrayList<String>();

    static {
        File[] files = Environment.getRootDirectory().listFiles();
        if (null != files) {
            for (final File f : files) {
                if (f.isDirectory()) {
                    try {
                        final String cp = f.getCanonicalPath();
                        final String ap = f.getAbsolutePath();
                        if (!cp.equals(ap)) {
                            aliases.add(ap);
                            aliasesPR.add(ap + "/");
                            mounts.add(cp);
                            mountsPR.add("/");
                        }
                    } catch (final IOException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            }
        }
    }

    private FileUtils() {
    }

    public static final String getRealPath(String absolutePath) {
        String sdcard = Environment.getExternalStorageDirectory().getPath();
        String filepath = absolutePath;
        if (absolutePath.contains(sdcard)) {
            filepath = absolutePath.substring(sdcard.length());
        }
        return filepath;
    }

    public static final String getStoragePath(String path) {
        return Environment.getExternalStorageDirectory().getPath() + "/" + (path);
    }

    public static final File getStorageDir(String dir) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/" + dir;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static final String getDir(File file) {
        if (file == null) {
            return "";
        }
        final String name = file.getName();
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - name.length());
    }

    public static final String getDir(String absPath) {
        if (absPath == null) {
            return "";
        }
        final int index = absPath.lastIndexOf("/");
        if (index == -1) {
            return "";
        }
        return absPath.substring(0, index + 1);
    }

    public static final String getFileSize(final long size) {
        if (size > 1073741824) {
            return String.format("%.2f", size / 1073741824.0) + " GB";
        } else if (size > 1048576) {
            return String.format("%.2f", size / 1048576.0) + " MB";
        } else if (size > 1024) {
            return String.format("%.2f", size / 1024.0) + " KB";
        } else {
            return size + " B";
        }

    }

    public static final String getFileDate(final long time) {
        return new SimpleDateFormat("dd MMM yyyy").format(time);
    }

    public static final String getAbsolutePath(final File file) {
        return file != null ? file.getAbsolutePath() : null;
    }

    public static final String getCanonicalPath(final File file) {
        try {
            return file != null ? file.getCanonicalPath() : null;
        } catch (final IOException ex) {
            return null;
        }
    }

    public static final String invertMountPrefix(final String fileName) {
        for (int i = 0, n = Math.min(aliases.size(), mounts.size()); i < n; i++) {
            final String alias = aliases.get(i);
            final String mount = mounts.get(i);
            if (fileName.equals(alias)) {
                return mount;
            }
            if (fileName.equals(mount)) {
                return alias;
            }
        }
        for (int i = 0, n = Math.min(aliasesPR.size(), mountsPR.size()); i < n; i++) {
            final String alias = aliasesPR.get(i);
            final String mount = mountsPR.get(i);
            if (fileName.startsWith(alias)) {
                return mount + fileName.substring(alias.length());
            }
            if (fileName.startsWith(mount)) {
                return alias + fileName.substring(mount.length());
            }
        }
        return null;
    }

    public static final String getName(final String absPath) {
        if (absPath == null) {
            return "";
        }
        final int index = absPath.lastIndexOf("/");
        if (index == -1) {
            return "";
        }
        return absPath.substring(index + 1);
    }

    public static final String getExtension(final File file) {
        if (file == null) {
            return "";
        }
        final String name = file.getName();
        final int index = name.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return name.substring(index + 1);
    }

    public static final FilePath parseFilePath(final String path, final Collection<String> extensions) {
        final File file = new File(path);
        final FilePath result = new FilePath();
        result.path = LengthUtils.safeString(file.getParent());
        result.name = file.getName();

        for (final String ext : extensions) {
            final String dext = "." + ext;
            if (result.name.endsWith(dext)) {
                result.extWithDot = dext;
                result.name = result.name.substring(0, result.name.length() - ext.length() - 1);
                break;
            }
        }
        return result;
    }

    public static void copy(final File source, final File target) throws IOException {
        if (!source.exists()) {
            return;
        }
        final long length = source.length();
        final int bufsize = MathUtils.adjust((int) length, 1024, 512 * 1024);

        final byte[] buf = new byte[bufsize];
        int l = 0;
        InputStream ins = null;
        OutputStream outs = null;
        try {
            ins = new FileInputStream(source);
            outs = new FileOutputStream(target);
            for (l = ins.read(buf); l > -1; l = ins.read(buf)) {
                outs.write(buf, 0, l);
            }
        } finally {
            if (outs != null) {
                try {
                    outs.close();
                } catch (final IOException ex) {
                }
            }
            if (ins != null) {
                try {
                    ins.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

    public static int move(final File sourceDir, final File targetDir, final String[] fileNames) {
        int count = 0;
        int processed = 0;
        final int updates = Math.max(1, fileNames.length / 20);

        boolean renamed = true;

        final byte[] buf = new byte[128 * 1024];
        int length = 0;
        for (final String file : fileNames) {
            final File source = new File(sourceDir, file);
            final File target = new File(targetDir, file);
            processed++;

            renamed = renamed && source.renameTo(target);
            if (renamed) {
                count++;
                continue;
            }

            try {
                InputStream ins = null;
                OutputStream outs = null;
                try {
                    ins = new FileInputStream(source);
                    outs = new FileOutputStream(target);
                    for (length = ins.read(buf); length > -1; length = ins.read(buf)) {
                        outs.write(buf, 0, length);
                    }
                } finally {
                    if (outs != null) {
                        try {
                            outs.close();
                        } catch (final IOException ex) {
                        }
                    }
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (final IOException ex) {
                        }
                    }
                }
                source.delete();
                count++;
            } catch (final IOException ex) {
                System.err.println(ex.getMessage());
            }
        }
        return count;
    }

    public static void copy(final InputStream source, final OutputStream target) throws IOException {
        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(source);
            out = Channels.newChannel(target);
            final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
            while (in.read(buf) > 0) {
                buf.flip();
                out.write(buf);
                buf.flip();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

    public static void copy(final InputStream source, final OutputStream target, final int bufsize,
                            final CopingProgress progress) throws IOException {
        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(source);
            out = Channels.newChannel(target);
            final ByteBuffer buf = ByteBuffer.allocateDirect(bufsize);
            long read = 0;
            while (in.read(buf) > 0) {
                buf.flip();
                read += buf.remaining();
                if (progress != null) {
                    progress.progress(read);
                }
                out.write(buf);
                buf.flip();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

    public static String readAssetAsString(String assetName) {
        try {
            AssetManager assetManager = App.Companion.getInstance().getAssets();
            InputStream is = assetManager.open(assetName);
            return StreamUtils.readStringFromInputStream(is);
        } catch (IOException e) {
            return null;
        }
    }

    public static interface CopingProgress {

        void progress(long bytes);
    }

    public static final class FilePath {

        public String path;
        public String name;
        public String extWithDot;

        public File toFile() {
            return new File(path, name + LengthUtils.safeString(extWithDot));
        }
    }

    public static final File getDiskCacheDir(final Context context, final String uniqueName) {
        final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()) ? getExternalCacheDir(
                context).getPath() : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Get the external app cache directory
     *
     * @param context The {@link Context} to use
     * @return The external cache directory
     */
    public static final File getExternalCacheDir(final Context context) {
        final File mCacheDir = context.getExternalCacheDir();
        if (mCacheDir != null) {
            return mCacheDir;
        }

        /* Before Froyo we need to construct the external cache dir ourselves */
        final String dir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + dir);
    }

    //---------------------------

    public static String MD5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes());
            return bytesToHexString(bytes);
        } catch (NoSuchAlgorithmException e) {
        }
        return data;
    }

    /**
     * 文件MD5值
     *
     * @param filepath
     */
    String md5File(String filepath) {
        try {
            File file = new File(filepath);
            FileInputStream fis = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int length = -1;
            while (fis.read(buffer, 0, 1024) != -1) {
                length = fis.read(buffer, 0, 1024);
                md.update(buffer, 0, length);
            }
            BigInteger bigInt = new BigInteger(1, md.digest());
            return bigInt.toString(16);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
