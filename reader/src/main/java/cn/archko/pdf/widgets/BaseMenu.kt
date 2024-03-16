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

const val type_reflow = 1
const val type_crop = 2
const val type_ocr = 3
const val type_outline = 4
const val type_seek = 5
const val type_scroll_ori = 6
const val type_font = 7
const val type_exit = 8