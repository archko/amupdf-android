package cn.archko.pdf.common

import android.graphics.Color
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cn.archko.pdf.utils.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * @author: archko 2021/10/4 :08:37
 */
class PdfOptionRepository(private val dataStore: DataStore<Preferences>) {

    private val TAG: String = "PdfPreferencesRepo"

    val pdfOptionFlow: Flow<PdfOption> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val showExtension = preferences[PdfOptionKeys.PREF_SHOW_EXTENSION] ?: true
            val imageOcr = preferences[PdfOptionKeys.PREF_OCR] ?: true
            val fullscreen = preferences[PdfOptionKeys.PREF_FULLSCREEN] ?: true
            val autocrop = preferences[PdfOptionKeys.PREF_AUTOCROP] ?: true
            val verticalScrollLock = preferences[PdfOptionKeys.PREF_VERTICAL_SCROLL_LOCK] ?: true
            val sideMargins2 = preferences[PdfOptionKeys.PREF_SIDE_MARGINS] ?: "0"
            val topMargin = preferences[PdfOptionKeys.PREF_TOP_MARGIN] ?: "10"
            val keepOn = preferences[PdfOptionKeys.PREF_KEEP_ON] ?: false
            val listStyle = preferences[PdfOptionKeys.PREF_LIST_STYLE] ?: "0"
            val dartTheme = preferences[PdfOptionKeys.PREF_DART_THEME] ?: false
            val orientation = preferences[PdfOptionKeys.PREF_ORIENTATION] ?: "7"
            val fontType = preferences[PdfOptionKeys.FONT_KEY_TYPE] ?: DEFAULT
            val fontName =
                preferences[PdfOptionKeys.FONT_KEY_NAME] ?: SYSTEM_FONT

            val textSize = preferences[PdfOptionKeys.STYLE_KEY_FONT_SIZE] ?: 16f
            val bgColor = preferences[PdfOptionKeys.STYLE_KEY_BGCOLOR] ?: Color.WHITE
            val fgColor = preferences[PdfOptionKeys.STYLE_KEY_FGCOLOR] ?: Color.BLACK
            val lineSpacingMult = preferences[PdfOptionKeys.STYLE_KEY_LINE_SPACEING_MULT] ?: 1.48f
            val leftPadding =
                preferences[PdfOptionKeys.STYLE_KEY_LEFT_PADDING] ?: Utils.dipToPixel(12f)
            val topPadding =
                preferences[PdfOptionKeys.STYLE_KEY_RIGHT_PADDING] ?: Utils.dipToPixel(16f)
            val rightPadding =
                preferences[PdfOptionKeys.STYLE_KEY_TOP_PADDING] ?: Utils.dipToPixel(12f)
            val bottomPadding =
                preferences[PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING] ?: Utils.dipToPixel(20f)
            val pdfPreferences = PdfOption(
                showExtension = showExtension,
                imageOcr = imageOcr,
                fullscreen = fullscreen,
                autocrop = autocrop,
                verticalScrollLock = verticalScrollLock,
                sideMargins2 = sideMargins2,
                topMargin = topMargin,
                keepOn = keepOn,
                listStyle = listStyle,
                dartTheme = dartTheme,
                orientation = orientation,
                fontType = fontType,
                textSize = textSize,
                fontName = fontName,
                bgColor = bgColor,
                fgColor = fgColor,
                lineSpacingMult = lineSpacingMult,
                leftPadding = leftPadding,
                topPadding = topPadding,
                rightPadding = rightPadding,
                bottomPadding = bottomPadding,
            )
            Logcat.d("pdfPreferences:$pdfPreferences ")
            pdfPreferences
        }

    suspend fun setShowExtension(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_SHOW_EXTENSION] = enable
        }
    }

    suspend fun setImageOcr(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_OCR] = enable
        }
    }

    suspend fun setFullscreen(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_FULLSCREEN] = enable
        }
    }

    suspend fun setAutocrop(autocrop: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_AUTOCROP] = autocrop
        }
    }

    suspend fun setVerticalScrollLock(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_VERTICAL_SCROLL_LOCK] = enable
        }
    }

    suspend fun setKeepOn(keepOn: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_KEEP_ON] = keepOn
        }
    }

    suspend fun setDartTheme(dartTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.PREF_DART_THEME] = dartTheme
        }
    }

    suspend fun setFontType(fontType: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.FONT_KEY_TYPE] = fontType
        }
    }

    suspend fun setFontName(fontName: String) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.FONT_KEY_NAME] = fontName
        }
    }

    suspend fun setTextSize(textSize: Float) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_FONT_SIZE] = textSize
        }
    }

    suspend fun setBgColor(bgColor: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_BGCOLOR] = bgColor
        }
    }

    suspend fun setFgColor(fgColor: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_FGCOLOR] = fgColor
        }
    }

    suspend fun setLineSpacingMult(lineSpacingMult: Float) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_LINE_SPACEING_MULT] = lineSpacingMult
        }
    }

    suspend fun setLeftPadding(leftPadding: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_LEFT_PADDING] = leftPadding
        }
    }

    suspend fun setTopPadding(topPadding: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_RIGHT_PADDING] = topPadding
        }
    }

    suspend fun setRightPadding(rightPadding: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_TOP_PADDING] = rightPadding
        }
    }

    suspend fun setBottomPadding(bottomPadding: Int) {
        dataStore.edit { preferences ->
            preferences[PdfOptionKeys.STYLE_KEY_BOTTOM_PADDING] = bottomPadding
        }
    }

    companion object {

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
}

data class PdfOption(
    val showExtension: Boolean = true,
    val imageOcr: Boolean = true,
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
) {

}

object PdfOptionKeys {
    val PREF_SHOW_EXTENSION = booleanPreferencesKey("showExtension")

    val PREF_ORIENTATION = stringPreferencesKey("orientation")
    val PREF_OCR = booleanPreferencesKey("image_ocr")
    val PREF_FULLSCREEN = booleanPreferencesKey("fullscreen")
    val PREF_AUTOCROP = booleanPreferencesKey("autocrop")
    val PREF_VERTICAL_SCROLL_LOCK = booleanPreferencesKey("verticalScrollLock")
    val PREF_SIDE_MARGINS = stringPreferencesKey("sideMargins2") // sideMargins was boolean

    val PREF_TOP_MARGIN = stringPreferencesKey("topMargin")
    val PREF_KEEP_ON = booleanPreferencesKey("keepOn")
    val PREF_LIST_STYLE = stringPreferencesKey("list_style")
    val PREF_DART_THEME = booleanPreferencesKey("pref_dart_theme")

    //============== font and style ==============
    val FONT_KEY_TYPE = intPreferencesKey("font_key_type")
    val FONT_KEY_NAME = stringPreferencesKey("font_key_name")

    val STYLE_KEY_FONT_SIZE = floatPreferencesKey("style_key_font_size")
    val STYLE_KEY_BGCOLOR = intPreferencesKey("style_key_bgcolor")
    val STYLE_KEY_FGCOLOR = intPreferencesKey("style_key_fgcolor")
    val STYLE_KEY_LINE_SPACEING_MULT = floatPreferencesKey("style_key_line_spaceing_mult")
    val STYLE_KEY_LEFT_PADDING = intPreferencesKey("style_key_left_padding")
    val STYLE_KEY_RIGHT_PADDING = intPreferencesKey("style_key_right_padding")
    val STYLE_KEY_TOP_PADDING = intPreferencesKey("style_key_top_padding")
    val STYLE_KEY_BOTTOM_PADDING = intPreferencesKey("style_key_bottom_padding")
}