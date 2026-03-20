package cn.archko.pdf.core.component

/**
 * @author: archko 2025/1/5 :07:53
 */
data class Size(
    val width: Int,
    val height: Int,
    val page: Int,
    val scale: Float = 1.0f,
    val offsetHeight: Int = 0,
)

data class IntSize(
    val width: Int,
    val height: Int,
)