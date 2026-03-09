package cn.archko.pdf.entity

import android.graphics.Color as AndroidColor

/**
 * 自定义Offset类，替代Compose的androidx.compose.ui.geometry.Offset
 * 保持与Compose Offset相同的API，以便JSON序列化/反序列化能正常工作
 * @author: archko 2026/3/9 :16:15
 */
public data class Offset(
    val x: Float,
    val y: Float
) {
    companion object {
        val Zero = Offset(0f, 0f)
    }
    
    operator fun plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
    operator fun times(scale: Float): Offset = Offset(x * scale, y * scale)
    operator fun div(scale: Float): Offset = Offset(x / scale, y / scale)
    
    fun distanceTo(other: Offset): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * Color包装类，替代Compose的androidx.compose.ui.graphics.Color
 * 保持与Compose Color相同的API（value属性），以便JSON序列化/反序列化能正常工作
 * 内部使用Android原生Color进行转换
 */
public data class Color(val value: Long) {
    companion object {
        val Red = Color(0xFFFF0000)
        val Green = Color(0xFF00FF00)
        val Blue = Color(0xFF0000FF)
        val Black = Color(0xFF000000)
        val White = Color(0xFFFFFFFF)
        val Transparent = Color(0x00000000)
        
        /**
         * 从Android原生Color创建
         */
        fun fromAndroidColor(color: Int): Color {
            // Android Color是ARGB格式，转换为Long
            return Color(color.toLong() and 0xFFFFFFFF)
        }
        
        /**
         * 从Android Color值创建（兼容Compose的Color(value)构造函数）
         */
        operator fun invoke(value: Long): Color = Color(value)
    }
    
    /**
     * 转换为Android原生Color
     */
    fun toAndroidColor(): Int = value.toInt()
    
    /**
     * 获取颜色分量（与Compose Color API兼容）
     */
    val alpha: Float get() = ((value shr 24) and 0xFF).toFloat() / 255f
    val red: Float get() = ((value shr 16) and 0xFF).toFloat() / 255f
    val green: Float get() = ((value shr 8) and 0xFF).toFloat() / 255f
    val blue: Float get() = (value and 0xFF).toFloat() / 255f
    
    /**
     * 修改透明度（与Compose Color API兼容）
     */
    fun withAlpha(alpha: Float): Color {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color((value and 0x00FFFFFF) or ((a.toLong() shl 24) and 0xFF000000))
    }
    
    override fun toString(): String = "#${value.toString(16).padStart(8, '0').uppercase()}"
}