package cn.archko.pdf.core.entity

import android.graphics.Color
import kotlin.math.sqrt

/**
 * 定义单条线段
 * @author: archko 2026/2/2 :16:35
 */
data class AnnotationPath(
    val points: List<Offset>,
    val config: PathConfig,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnotationPath

        if (points != other.points) return false
        if (config != other.config) return false

        return true
    }

    override fun hashCode(): Int {
        var result = points.hashCode()
        result = 31 * result + config.hashCode()
        return result
    }
}

data class PathConfig(
    var color: Int = Color.RED,
    var strokeWidth: Float = 4f,
    var drawType: DrawType = DrawType.LINE
) {
    override fun toString(): String {
        return "PathConfig(color=#${Integer.toHexString(color)}, strokeWidth=$strokeWidth, drawType=$drawType)"
    }
}

enum class DrawType { CURVE, LINE, CIRCLE }

data class Offset(
    val x: Float,
    val y: Float
) {
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(scale: Float): Offset = Offset(x * scale, y * scale)
    operator fun div(scale: Float): Offset = Offset(x / scale, y / scale)

    fun distanceTo(other: Offset): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}