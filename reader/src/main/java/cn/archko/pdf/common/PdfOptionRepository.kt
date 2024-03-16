package cn.archko.pdf.common

import android.graphics.Color
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils
import com.tencent.mmkv.MMKV

/**
 * @author: archko 2021/10/4 :08:37
 */
object PdfOptionRepository {

    private val TAG: String = "PdfPreferencesRepo"
    val mmkv = MMKV.defaultMMKV()

    fun setOrientation(ori: Int) {
        mmkv.encode(PdfOptionKeys.PREF_ORIENTATION, ori)
    }

    fun getOrientation(): Int {
        return mmkv.decodeInt(PdfOptionKeys.PREF_ORIENTATION, 7)
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

    fun getVerticalScrollLock(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_VERTICAL_SCROLL_LOCK, true)
    }

    fun setKeepOn(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_KEEP_ON, enable)
    }

    fun getKeepOn(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_KEEP_ON, true)
    }

    fun setDartTheme(dartTheme: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_DART_THEME, dartTheme)
    }

    fun getDartTheme(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_DART_THEME, false)
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
        return mmkv.decodeFloat(PdfOptionKeys.STYLE_KEY_FONT_SIZE, 16f)
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
        return mmkv.decodeFloat(PdfOptionKeys.STYLE_KEY_LINE_SPACEING_MULT, 1.48f)
    }

    fun setLeftPadding(leftPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_LEFT_PADDING, leftPadding)
    }

    fun getLeftPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_LEFT_PADDING, Utils.dipToPixel(12f))
    }

    fun setTopPadding(topPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_RIGHT_PADDING, topPadding)
    }

    fun getTopPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_RIGHT_PADDING, Utils.dipToPixel(16f))
    }

    fun setRightPadding(rightPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_TOP_PADDING, rightPadding)
    }

    fun getRightPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_TOP_PADDING, Utils.dipToPixel(12f))
    }

    fun setBottomPadding(bottomPadding: Int) {
        mmkv.encode(PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING, bottomPadding)
    }

    fun getBottomPadding(): Int {
        return mmkv.decodeInt(PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING, Utils.dipToPixel(16f))
    }

    fun setDirsFirst(enable: Boolean): Boolean {
        return mmkv.encode(PdfOptionKeys.PREF_DIRS_FIRST, enable)
    }

    fun getDirsFirst(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_DIRS_FIRST, true)
    }

    fun setNewViewer(enable: Boolean) {
        mmkv.encode(PdfOptionKeys.PREF_NEW_VIEWER, enable)
    }

    fun getNewViewer(): Boolean {
        return mmkv.decodeBool(PdfOptionKeys.PREF_NEW_VIEWER, false)
    }

    fun setCropper(enable: Boolean) {
        val code = if (enable) 1 else 0
        MupdfDocument.useNewCropper = enable
        mmkv.encode(PdfOptionKeys.PREF_CROPPER, code)
    }

    fun getCropper(): Boolean {
        return mmkv.decodeInt(PdfOptionKeys.PREF_CROPPER, 0) == 1
    }

    @JvmField
    val FONT_DIR = "amupdf/fonts/"

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
    var leftPadding: Int = Utils.dipToPixel(12f),
    var topPadding: Int = Utils.dipToPixel(16f),
    var rightPadding: Int = Utils.dipToPixel(12f),
    var bottomPadding: Int = Utils.dipToPixel(20f),
)

object PdfOptionKeys {
    const val PREF_SHOW_EXTENSION = ("showExtension")

    const val PREF_ORIENTATION = ("orientation")
    const val PREF_OCR = ("image_ocr")
    const val PREF_OVERRIDE_FILE = ("override_file")
    const val PREF_FULLSCREEN = ("fullscreen")
    const val PREF_AUTOCROP = ("autocrop")
    const val PREF_VERTICAL_SCROLL_LOCK = ("verticalScrollLock")
    const val PREF_SIDE_MARGINS = ("sideMargins2") // sideMargins was boolean

    const val PREF_TOP_MARGIN = ("topMargin")
    const val PREF_KEEP_ON = ("keepOn")
    const val PREF_LIST_STYLE = ("list_style")
    const val PREF_DART_THEME = ("pref_dart_theme")
    const val PREF_DIRS_FIRST = ("dirsFirst")
    const val PREF_NEW_VIEWER = ("newViewer")
    const val PREF_CROPPER = ("cropper")

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