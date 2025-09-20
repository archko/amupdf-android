package cn.archko.pdf.common

import android.graphics.Color
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.entity.padding
import com.tencent.mmkv.MMKV

/**
 * @author: archko 2021/10/4 :08:37
 */
object PdfOptionRepository {

    private val mmkv: MMKV = MMKV.defaultMMKV()

    fun setOrientation(ori: Int) {
        mmkv.encode(PdfOptionKeys.PREF_ORIENTATION, ori)
    }

    fun getOrientation(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_ORIENTATION, 1)
    }

    fun setShowExtension(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_SHOW_EXTENSION, enable)
    }

    fun getShowExtension(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_SHOW_EXTENSION, true)
    }

    fun setImageOcr(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_OCR, enable)
    }

    fun getImageOcr(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_OCR, true)
    }

    fun setFullscreen(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_FULLSCREEN, enable)
    }

    fun getFullscreen(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_FULLSCREEN, true)
    }

    fun setAutocrop(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_AUTOCROP, enable)
    }

    fun getAutocrop(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_AUTOCROP, true)
    }

    fun setKeepOn(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_KEEP_ON, enable)
    }

    fun getKeepOn(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_KEEP_ON, true)
    }

    fun setFontType(fontType: Int) {
        mmkv.encode(PdfOptionKeys.FONT_KEY_TYPE, fontType)
    }

    fun getFontType(): Int {
        return mmkv.decodeInt(PdfOptionKeys.FONT_KEY_TYPE, DEFAULT)
    }

    fun setFontName(fontName: String) {
        mmkv.encode(PdfOptionKeys.FONT_KEY_NAME, fontName)
    }

    fun getFontName(): String? {
        return mmkv.decodeString(PdfOptionKeys.FONT_KEY_NAME, "")
    }

    fun setTextSize(textSize: Float) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_FONT_SIZE, textSize)
    }

    fun getTextSize(): Float {
        return mmkv.decodeFloat(PdfOptionKeys.STYLE_KEY_FONT_SIZE, 17f)
    }

    fun setBgColor(bgColor: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_BGCOLOR, bgColor)
    }

    fun getBgColor(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_BGCOLOR, Color.WHITE)
    }

    fun setFgColor(fgColor: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_FGCOLOR, fgColor)
    }

    fun getFgColor(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_FGCOLOR, Color.BLACK)
    }

    fun setLineSpacingMult(lineSpacingMult: Float) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_LINE_SPACEING_MULT, lineSpacingMult)
    }

    fun getLineSpacingMult(): Float {
        return mmkv.decodeFloat(PdfOptionKeys.STYLE_KEY_LINE_SPACEING_MULT, 1.4f)
    }

    fun setLeftPadding(leftPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_LEFT_PADDING, leftPadding)
    }

    fun getLeftPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_LEFT_PADDING, Utils.dipToPixel(padding))
    }

    fun setTopPadding(topPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_RIGHT_PADDING, topPadding)
    }

    fun getTopPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_RIGHT_PADDING, Utils.dipToPixel(padding))
    }

    fun setRightPadding(rightPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_TOP_PADDING, rightPadding)
    }

    fun getRightPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_TOP_PADDING, Utils.dipToPixel(padding))
    }

    fun setBottomPadding(bottomPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING, bottomPadding)
    }

    fun getBottomPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING, Utils.dipToPixel(padding))
    }

    fun setDirsFirst(enable: Boolean): Boolean {
        return mmkv.encode(PdfOptionKeys.PREF_DIRS_FIRST, enable)
    }

    fun getDirsFirst(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_DIRS_FIRST, true)
    }

    fun setColorMode(colorMode: Int) {
        mmkv.encode(PdfOptionKeys.PREF_COLORMODE, colorMode)
    }

    fun getColorMode(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_COLORMODE, 0)
    }

    fun setStyle(style: Int) {
        mmkv.encode(PdfOptionKeys.PREF_STYLE, style)
    }

    fun getStyle(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_STYLE, 0)
    }

    fun setLibraryStyle(style: Int) {
        mmkv.encode(PdfOptionKeys.PREF_LIBRARY_STYLE, style)
    }

    fun getLibraryStyle(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_LIBRARY_STYLE, 0)
    }

    fun setScanFolder(path: String) {
        mmkv.encode(PdfOptionKeys.PREF_SCAN_FOLDER, path)
    }

    fun getScanFolder(): String? {
        return mmkv.decodeString(PdfOptionKeys.PREF_SCAN_FOLDER, "")
    }

    fun setAutoScan(auto: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_AUTO_SCAN, auto)
    }

    fun getAutoScan(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_AUTO_SCAN, true)
    }

    fun setSort(sort: Int) {
        mmkv.encode(PdfOptionKeys.PREF_LIBRARY_SORT, sort)
    }

    fun getSort(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_LIBRARY_SORT, 0)
    }

    fun setDecodeBlock(sort: Int) {
        mmkv.encode(PdfOptionKeys.PREF_DECODE_BLOCK, sort)
    }

    fun getDecodeBlock(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_DECODE_BLOCK, 2)
    }

    @JvmField
    val FONT_DIR = "fonts/"

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

data class PdfOption(
    val showExtension: Boolean = true,
    val imageOcr: Boolean = true,
    val overrideFile: Boolean = true,
    val fullscreen: Boolean = true,
    val autocrop: Boolean = true,
    val verticalScrollLock: Boolean = true,
    val sideMargins2: String = "0",
    val topMargin: String = "0",
    val keepOn: Boolean = false,
    val listStyle: String = "0",
    val dartTheme: Boolean = true,
    val orientation: String = "7",
    val fontType: Int = PdfOptionRepository.DEFAULT,
    val fontName: String = PdfOptionRepository.SYSTEM_FONT,

    var textSize: Float = 16f,
    var bgColor: Int = Color.WHITE,
    var fgColor: Int = Color.BLACK,
    var lineSpacingMult: Float = 1.48f,
    var leftPadding: Int = Utils.dipToPixel(padding),
    var topPadding: Int = Utils.dipToPixel(padding),
    var rightPadding: Int = Utils.dipToPixel(padding),
    var bottomPadding: Int = Utils.dipToPixel(padding),
)

object PdfOptionKeys {
    const val PREF_SHOW_EXTENSION = ("showExtension")

    const val PREF_ORIENTATION = ("orientation")
    const val PREF_OCR = ("image_ocr")
    const val PREF_FULLSCREEN = ("fullscreen")
    const val PREF_AUTOCROP = ("autocrop")

    const val PREF_LEFT_MARGIN = ("leftMargin")
    const val PREF_TOP_MARGIN = ("topMargin")
    const val PREF_RIGHT_MARGIN = ("rightMargin")
    const val PREF_BOTTOM_MARGIN = ("bottomMargin")
    const val PREF_KEEP_ON = ("keepOn")
    const val PREF_DIRS_FIRST = ("dirsFirst")
    const val PREF_COLORMODE = ("colorMode")
    const val PREF_STYLE = ("style")
    const val PREF_LIBRARY_STYLE = ("libraryStyle")
    const val PREF_AUTO_SCAN = ("autoScan")
    const val PREF_SCAN_FOLDER = ("scanFolder")
    const val PREF_LIBRARY_SORT = ("prefLibrarySort")
    const val PREF_DECODE_BLOCK = ("decodeBlock")

    //============== font and style ==============
    const val FONT_KEY_TYPE = ("font_key_type")
    const val FONT_KEY_NAME = ("font_key_name")

    const val STYLE_KEY_FONT_SIZE = ("style_key_font_size")
    const val STYLE_KEY_BGCOLOR = ("style_key_bgcolor")
    const val STYLE_KEY_FGCOLOR = ("style_key_fgcolor")
    const val STYLE_KEY_LINE_SPACEING_MULT = ("style_key_line_spaceing_mult")
    const val STYLE_KEY_LEFT_PADDING = ("style_key_left_padding")
    const val STYLE_KEY_RIGHT_PADDING = ("style_key_right_padding")
    const val STYLE_KEY_TOP_PADDING = ("style_key_top_padding")
    const val STYLE_KEY_BOTTOM_PADDING = ("style_key_bottom_padding")
}