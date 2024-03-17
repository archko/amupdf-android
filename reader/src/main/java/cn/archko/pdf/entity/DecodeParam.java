package cn.archko.pdf.entity;

import android.text.TextUtils;
import android.widget.ImageView;

import com.artifex.mupdf.fitz.Document;

import cn.archko.pdf.listeners.DecodeCallback;

/**
 * @author: archko 2024/3/17 :19:41
 */
public final class DecodeParam {
    public String key;
    public int pageNum;
    public float zoom;
    public int screenWidth;
    public ImageView imageView;
    public boolean crop;
    public int xOrigin;
    public APage pageSize;
    public Document document;
    public int targetWidth;
    public DecodeCallback decodeCallback;

    public DecodeParam(String key, int pageNum, float zoom, int screenWidth, ImageView imageView) {
        this.key = key;
        this.pageNum = pageNum;
        this.zoom = zoom;
        this.screenWidth = screenWidth;
        this.imageView = imageView;
    }

    public DecodeParam(String key, ImageView imageView, boolean crop, int xOrigin,
                       APage pageSize, Document document, DecodeCallback callback) {
        this.key = key;
        pageNum = pageSize.index;
        if (TextUtils.isEmpty(key)) {
            this.key = String.format("%s,%s,%s,%s", imageView, crop, xOrigin, pageSize);
        }
        this.imageView = imageView;
        this.crop = crop;
        this.xOrigin = xOrigin;
        this.pageSize = pageSize;
        this.document = document;
        this.decodeCallback = callback;
    }

    @Override
    public String toString() {
        return "DecodeParam{" +
                "key='" + key + '\'' +
                ", pageNum=" + pageNum +
                ", zoom=" + zoom +
                ", screenWidth=" + screenWidth +
                ", imageView=" + imageView +
                ", crop=" + crop +
                ", xOrigin=" + xOrigin +
                ", pageSize=" + pageSize +
                ", document=" + document +
                ", targetWidth=" + targetWidth +
                ", decodeCallback=" + decodeCallback +
                '}';
    }
}