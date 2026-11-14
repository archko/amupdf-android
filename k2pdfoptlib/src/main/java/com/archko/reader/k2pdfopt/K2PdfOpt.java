package com.archko.reader.k2pdfopt;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.Map;

/**
 * 基于 koreader libk2pdfopt 的单页重排接口。
 * 一次 load() 会把源页重排成一张竖长图片, 通过 renderTile() 按块取图。
 */
public class K2PdfOpt {

    static {
        if (Config.natives) {
            System.loadLibrary("k2pdfoptjni");
            Config.natives = false;
        }
    }

    private long handle;

    public K2PdfOpt() {
    }

    /**
     * 设置目标设备尺寸, 必须在 load() 之前调用。
     *
     * @param width  设备/页面的宽, 像素
     * @param height 设备/页面的高, 像素
     * @param dpi    目标 dpi
     */
    public native void create(int width, int height, int dpi);

    /**
     * 输出字号倍率, 1.0 为原始字号。
     */
    public native void setFontSize(float size);

    public native float getFontSize();

    /**
     * 重排时告诉 k2pdfopt 的源图 dpi, 用于推算字号和行宽, 不是屏幕密度。
     * 与源图渲染倍率一起决定页面在重排中的物理宽度。
     */
    public native void setSourceDpi(float dpi);

    /**
     * 重排页边距, 英寸。
     */
    public native void setMargin(float margin);

    /**
     * 最大分栏数, 1 为不分栏。
     */
    public native void setColumns(int columns);

    /**
     * 源页缩放质量, 未设置 sourceDpi 时使用。
     */
    public native void setQuality(float quality);

    public native void setWrap(boolean wrap);

    public native void setTrim(boolean trim);

    /**
     * 字间距(分词/断行阈值), 数值为小写字母高度的比例。
     * 负值表示自动, 绝对值作为最小间距; 值越大越不容易断行。
     */
    public native void setWordSpacing(float spacing);

    /**
     * 重排一页源图。结果保留在 native 端, 直到下一次 load()/close()。
     */
    public native void load(Bitmap src);

    /**
     * 重排后整页的宽, 像素。
     */
    public native int getPageWidth();

    /**
     * 重排后整页的高, 像素。
     */
    public native int getPageHeight();

    /**
     * 重排后整页, 注意可能很大, 优先使用 renderTile()。
     */
    public native Bitmap renderPage();

    /**
     * 从重排后的长图中取一个矩形块。
     */
    public native Bitmap renderTile(int x, int y, int width, int height);

    /**
     * 释放 native 端的重排结果, Java 端已经取出的 Bitmap 不受影响。
     */
    public native void releasePage();

    /**
     * 源页 -> 重排页的矩形映射, 用于进度/高亮。
     */
    public native Map<Rect, Rect> getRectMaps();

    public native void close();
}
