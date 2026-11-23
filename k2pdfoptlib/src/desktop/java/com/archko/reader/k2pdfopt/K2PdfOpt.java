package com.archko.reader.k2pdfopt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * 桌面版 libk2pdfopt JNI 封装。
 * 动态库随 JAR 一起发布, 优先从 java.library.path 加载, 找不到时从 JAR 解压到临时目录加载。
 */
public class K2PdfOpt {

    static {
        loadNativeLibrary();
    }

    private long handle;

    public K2PdfOpt() {
    }

    private static void loadNativeLibrary() {
        String libName = "k2pdfopt_jni";
        String mapped = System.mapLibraryName(libName);
        try {
            System.loadLibrary(libName);
            return;
        } catch (UnsatisfiedLinkError ignored) {
        }
        String resource = "/natives/" + osName() + "-" + archName() + "/" + mapped;
        try (InputStream in = K2PdfOpt.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new UnsatisfiedLinkError("native library not found in JAR: " + resource);
            }
            Path dir = Files.createTempDirectory("k2pdfopt-jni");
            Path target = dir.resolve(mapped);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
            dir.toFile().deleteOnExit();
            System.load(target.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new UnsatisfiedLinkError("failed to extract native library " + resource + ": " + e);
        }
    }

    private static String osName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return "macos";
        }
        if (os.contains("win")) {
            return "windows";
        }
        return "linux";
    }

    private static String archName() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "arm64";
        }
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "x86_64";
        }
        return arch.replaceAll("[^a-z0-9_]", "");
    }

    public native void create(int width, int height, int dpi);

    public native void setFontSize(float size);

    public native float getFontSize();

    public native void setSourceDpi(float dpi);

    public native void setMargin(float margin);

    public native void setColumns(int columns);

    public native void setQuality(float quality);

    public native void setWrap(boolean wrap);

    public native void setTrim(boolean trim);

    public native void setWordSpacing(float spacing);

    public native void loadBuffer(int[] argbPixels, int width, int height);

    public native int[] renderPageBuffer();

    public native int[] renderTileBuffer(int x, int y, int width, int height);

    public native int getPageWidth();

    public native int getPageHeight();

    public native void releasePage();

    public native void close();

    public void loadBufferedImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        loadBuffer(pixels, width, height);
    }

    public BufferedImage renderPageBufferedImage() {
        return toBufferedImage(renderPageBuffer(), getPageWidth(), getPageHeight());
    }

    public BufferedImage renderTileBufferedImage(int x, int y, int width, int height) {
        int actualWidth = Math.min(width, Math.max(getPageWidth() - x, 0));
        int actualHeight = Math.min(height, Math.max(getPageHeight() - y, 0));
        return toBufferedImage(renderTileBuffer(x, y, width, height), actualWidth, actualHeight);
    }

    private static BufferedImage toBufferedImage(int[] pixels, int width, int height) {
        if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height) {
            return null;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
}
