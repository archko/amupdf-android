package cn.archko.pdf.entity

import android.graphics.Bitmap

/**
 * @author: archko 2019/11/30 :3:18 PM
 */
class BitmapBean @JvmOverloads constructor(
    var bitmap: Bitmap,
    var index: Int,
    var width: Float = bitmap.width.toFloat(),
    var height: Float = bitmap.height.toFloat()
) {
    constructor(bitmap: Bitmap, width: Float, height: Float) :
            this(bitmap, 0, width, height) {
    }

    override fun toString(): String {
        return "FileBean{" +
                "index:" + index +
                ",bitmap:" + bitmap +
                '}'
    }
}