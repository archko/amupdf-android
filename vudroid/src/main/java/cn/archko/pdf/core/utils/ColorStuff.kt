package cn.archko.pdf.core.utils

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * @author: archko 2024/7/13 :14:28
 * copy from orion-viewer
 */
class ColorStuff {

    val borderPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 0f
        style = Paint.Style.STROKE
    }

    val backgroundPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    val pagePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        color = Color.WHITE
    }

    private var transformationArray: FloatArray? = null

    fun setColorMatrix(colorMatrix: FloatArray?) {
        transformationArray = colorMatrix
        val filter =
            if (colorMatrix != null) ColorMatrixColorFilter(ColorMatrix(colorMatrix)) else null
        backgroundPaint.colorFilter = filter
        borderPaint.colorFilter = filter
        pagePaint.colorFilter = filter
    }
}