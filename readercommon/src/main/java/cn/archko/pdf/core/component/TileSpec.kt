package cn.archko.pdf.core.component

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * @author: archko 2025/7/26 :10:58
 */
data class TileSpec(
    val page: Int,
    val pageScale: Float,
    val bounds: RectF, // 0~1
    val pageWidth: Int,
    val pageHeight: Int,
    val viewSize: IntSize,
    val cacheKey: String,
    var imageBitmap: Bitmap?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileSpec

        if (page != other.page) return false
        if (pageScale != other.pageScale) return false
        if (pageWidth != other.pageWidth) return false
        if (pageHeight != other.pageHeight) return false
        if (bounds != other.bounds) return false
        if (cacheKey != other.cacheKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = page
        result = 31 * result + pageScale.hashCode()
        result = 31 * result + pageWidth
        result = 31 * result + pageHeight
        result = 31 * result + bounds.hashCode()
        result = 31 * result + cacheKey.hashCode()
        return result
    }
}