package cn.archko.pdf.common

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import cn.archko.pdf.App
import cn.archko.pdf.entity.FontBean

import java.io.File

import cn.archko.pdf.utils.FileUtils

/**
 * @author: archko 2019-06-19 :12:27
 */
class FontHelper {

    init {
        loadFont()
    }

    companion object {

        @JvmField
        val FONT_DIR = "amupdf/fonts/"

        @JvmField
        val FONT_SP_FILE = "font_sp_file"

        @JvmField
        val FONT_KEY_TYPE = "font_key_type"

        @JvmField
        val FONT_KEY_NAME = "font_key_name"

        @JvmField
        val SYSTEM_FONT = "System Font"

        @JvmField
        val SYSTEM_FONT_SAN = "System Font SAN"

        @JvmField
        val SYSTEM_FONT_SERIF = "System Font SERIF"

        @JvmField
        val SYSTEM_FONT_MONO = "System Font MONO"

        val DEFAULT = 0
        val DEFAULT_BOLD = 1
        val SANS_SERIF = 2
        val SERIF = 3
        val MONOSPACE = 4
        val CUSTOM = 5
    }

    var fontBean: FontBean? = null
    var typeface: Typeface? = null
        get() {
            if (field == null) {
                loadFont()
            }

            return field
        }

    fun loadFont() {
        val sp: SharedPreferences =
            App.instance!!.getSharedPreferences(FONT_SP_FILE, Context.MODE_PRIVATE)
        val fontType = sp.getInt(FONT_KEY_TYPE, DEFAULT)
        val fontName = sp.getString(FONT_KEY_NAME, SYSTEM_FONT)
        initFontBean(fontType, fontName)
    }

    private fun initFontBean(fontType: Int, fontName: String?) {
        if (null == fontBean) {
            fontBean = FontBean(fontType, fontName, null)
        }
        fontBean?.fontType = fontType
        fontBean?.fontName = fontName

        if (fontType == CUSTOM) {
            fontBean?.file = File(FileUtils.getStoragePath(FONT_DIR + fontName))
            typeface = createFont(fontName)
        } else {
            when (fontType) {
                DEFAULT -> typeface = Typeface.DEFAULT
                SANS_SERIF -> typeface = Typeface.SANS_SERIF
                SERIF -> typeface = Typeface.SERIF
                MONOSPACE -> typeface = Typeface.MONOSPACE
            }
        }
    }

    fun saveFont(fBean: FontBean) {
        if ((fontBean?.fontType == CUSTOM && !fBean.fontName.equals(fontBean?.fontName))
            || fBean.fontType != fontBean?.fontType
        ) {
            initFontBean(fBean.fontType, fBean.fontName)
        }
        val sp: SharedPreferences =
            App.instance!!.getSharedPreferences(FONT_SP_FILE, Context.MODE_PRIVATE)
        sp.edit()
            .putInt(FONT_KEY_TYPE, fontBean?.fontType!!)
            .putString(FONT_KEY_NAME, fontBean?.fontName)
            .apply()
    }

    fun createFont(fontName: String?): Typeface? {
        val fontPath = FileUtils.getStoragePath(FONT_DIR + fontName)
        return if (!File(fontPath).exists()) {
            null
        } else Typeface.createFromFile(fontPath)
    }

    fun createFontByPath(fontPath: String): Typeface? {
        return Typeface.createFromFile(fontPath)
    }
}
