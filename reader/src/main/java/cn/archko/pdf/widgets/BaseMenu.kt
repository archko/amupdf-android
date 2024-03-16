package cn.archko.pdf.widgets

import android.graphics.Color

/**
 * @author: archko 2022/7/30 :08:28
 */
data class BaseMenu(
    var color: Int = Color.BLUE,
    var percent: Float = 10f,
    var content: String? = null,
    var resId: Int = -1,
    var type: Int = 0,
)