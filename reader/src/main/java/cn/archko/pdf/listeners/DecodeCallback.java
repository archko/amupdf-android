package cn.archko.pdf.listeners;

import android.graphics.Bitmap;

import cn.archko.pdf.common.ImageWorker;
import cn.archko.pdf.entity.DecodeParam;

/**
 * @author: archko 2019/12/25 :10:18 下午
 */
public interface DecodeCallback {
    void decodeComplete(Bitmap bitmap, DecodeParam param);
    boolean shouldRender(int index, DecodeParam param);
}