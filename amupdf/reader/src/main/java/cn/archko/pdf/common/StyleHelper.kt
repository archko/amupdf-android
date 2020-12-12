package cn.archko.pdf.common

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import cn.archko.pdf.App
import cn.archko.pdf.entity.StyleBean
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2019-06-19 :12:27
 */
class StyleHelper {

    companion object {

        @JvmField
        val STYLE_SP_FILE = "style_sp_file"
        val STYLE_KEY_FONT_SIZE = "style_key_font_size"
        val STYLE_KEY_BGCOLOR = "style_key_bgcolor"
        val STYLE_KEY_FGCOLOR = "style_key_fgcolor"
        val STYLE_KEY_LINE_SPACEING_MULT = "style_key_line_spaceing_mult"
        val STYLE_KEY_LEFT_PADDING = "style_key_left_padding"
        val STYLE_KEY_RIGHT_PADDING = "style_key_right_padding"
        val STYLE_KEY_TOP_PADDING = "style_key_top_padding"
        val STYLE_KEY_BOTTOM_PADDING = "style_key_bottom_padding"

    }

    var styleBean: StyleBean? = null
        get() {
            return field
        }

    var fontHelper: FontHelper? = null
        get() {
            if (field == null) {
                fontHelper = FontHelper()
            }

            return field
        }

    init {
        loadStyle()
    }

    private fun loadStyle() {
        fontHelper = FontHelper()
        fontHelper?.loadFont()
        loadStyleFromSP()
    }

    fun loadStyleFromSP() {
        val sp: SharedPreferences =
            App.instance!!.getSharedPreferences(STYLE_SP_FILE, Context.MODE_PRIVATE)
        val textSize: Float = sp.getFloat(STYLE_KEY_FONT_SIZE, 16f)
        val bgColor: Int = sp.getInt(STYLE_KEY_BGCOLOR, Color.WHITE)
        val fgColor: Int = sp.getInt(STYLE_KEY_FGCOLOR, Color.BLACK)
        val lineSpacingMult: Float = sp.getFloat(STYLE_KEY_LINE_SPACEING_MULT, 1.48f)
        val leftPadding: Int = sp.getInt(STYLE_KEY_LEFT_PADDING, Utils.dipToPixel(12f))
        val topPadding: Int = sp.getInt(STYLE_KEY_TOP_PADDING, Utils.dipToPixel(16f))
        val rightPadding: Int = sp.getInt(STYLE_KEY_RIGHT_PADDING, Utils.dipToPixel(12f))
        val bottomPadding: Int = sp.getInt(STYLE_KEY_BOTTOM_PADDING, Utils.dipToPixel(20f))
        styleBean = StyleBean(
            textSize,
            bgColor,
            fgColor,
            lineSpacingMult,
            leftPadding,
            topPadding,
            rightPadding,
            bottomPadding
        )
    }

    fun saveStyleToSP(sBean: StyleBean?) {
        sBean?.run {
            styleBean = sBean
            val sp: SharedPreferences =
                App.instance!!.getSharedPreferences(STYLE_SP_FILE, Context.MODE_PRIVATE)
            sp.edit()
                .putFloat(STYLE_KEY_FONT_SIZE, sBean.textSize)
                .putInt(STYLE_KEY_BGCOLOR, sBean.bgColor)
                .putInt(STYLE_KEY_FGCOLOR, sBean.fgColor)
                .putFloat(STYLE_KEY_LINE_SPACEING_MULT, sBean.lineSpacingMult)
                .putInt(STYLE_KEY_LEFT_PADDING, sBean.leftPadding)
                .putInt(STYLE_KEY_TOP_PADDING, sBean.topPadding)
                .putInt(STYLE_KEY_RIGHT_PADDING, sBean.rightPadding)
                .putInt(STYLE_KEY_BOTTOM_PADDING, sBean.bottomPadding)
                .apply()
        }
    }
}
