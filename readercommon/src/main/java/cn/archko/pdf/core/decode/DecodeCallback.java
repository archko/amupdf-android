package cn.archko.pdf.core.decode;

import android.graphics.Bitmap;

import cn.archko.pdf.core.decode.DecodeParam;

/**
 * @author: archko 2019/12/25 :10:18 下午
 */
public interface DecodeCallback {
    void decodeComplete(Bitmap bitmap, DecodeParam param);
    boolean shouldRender(int index, DecodeParam param);
}