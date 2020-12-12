package cn.archko.pdf.entity

import android.graphics.Color
import cn.archko.pdf.App
import cn.archko.pdf.R
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2019/7/12 :19:50
 */
data class StyleBean(
    var textSize: Float = 16f,
    var bgColor: Int = Color.WHITE,
    var fgColor: Int = Color.BLACK,
    var lineSpacingMult: Float = 1.48f,
    var leftPadding: Int = Utils.dipToPixel(12f),
    var topPadding: Int = Utils.dipToPixel(16f),
    var rightPadding: Int = Utils.dipToPixel(12f),
    var bottomPadding: Int = Utils.dipToPixel(20f)
) {
}