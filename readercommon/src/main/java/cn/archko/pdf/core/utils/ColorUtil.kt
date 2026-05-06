package cn.archko.pdf.core.utils

import cn.archko.pdf.core.common.PdfOptionRepository

/**
 * copy from orion-viewer
 */
object ColorUtil {

    private val COLOR_MATRICES: Map<Int, FloatArray?> = linkedMapOf(
        0 to null,

        1 to floatArrayOf(
            -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        ),

        //"CM_BLACK_ON_YELLOWISH"
        2 to floatArrayOf(
            0.94f, 0.02f, 0.02f, 0.0f, 0.0f,
            0.02f, 0.86f, 0.02f, 0.0f, 0.0f,
            0.02f, 0.02f, 0.74f, 0.0f, 0.0f,
            0.00f, 0.00f, 0.00f, 1.0f, 0.0f
        ),

        //"CM_BLACK_ON_green"//苹果绿底
        3 to floatArrayOf(
            0.83f, 0.00f, 0.00f, 0.0f, 0.0f,
            0.00f, 0.96f, 0.00f, 0.0f, 0.0f,
            0.00f, 0.00f, 0.84f, 0.0f, 0.0f,
            0.00f, 0.00f, 0.00f, 1.0f, 0.0f
        ),

        //"CM_GRAYSCALE_LIGHT"
        4 to floatArrayOf(
            0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
            0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
            0.27f, 0.54f, 0.09f, 0.0f, 0.0f,
            0.00f, 0.00f, 0.00f, 1.0f, 0.0f
        ),

        //"CM_GRAYSCALE"
        5 to floatArrayOf(
            0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
            0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
            0.215f, 0.45f, 0.08f, 0.0f, 0.0f,
            0.000f, 0.00f, 0.00f, 1.0f, 0.0f
        ),

        //"CM_GRAYSCALE_DARK"
        6 to floatArrayOf(
            0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
            0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
            0.15f, 0.30f, 0.05f, 0.0f, 0.0f,
            0.00f, 0.00f, 0.00f, 1.0f, 0.0f
        ),

        //"CM_CUSTOM_MATRIX" - 自定义矩阵（占位符，实际由用户选择）
        7 to null
    )

    @JvmStatic
    fun getColorMode(type: Int): FloatArray? {
        return if (type == 7) {
            // 自定义矩阵，从PdfOptionRepository获取
            PdfOptionRepository.getCustomColorMatrix() ?: COLOR_MATRICES[type]
        } else {
            COLOR_MATRICES[type]
        }
    }

    /*@ColorInt
    @JvmStatic
    fun transformColor(color: Int, transformation: FloatArray): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val a = Color.alpha(color)

        val array = IntArray(4)
        for (i in array.indices) {
            val shift = i * 5
            array[i] = (r * transformation[shift + 0] +
                    g * transformation[shift + 1] +
                    b * transformation[shift + 2] +
                    a * transformation[shift + 3] +
                    transformation[shift + 4]
                    ).toInt()
        }
        return Color.argb(array[3], array[0], array[1], array[2])
    }*/
}
