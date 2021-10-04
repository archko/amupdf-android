package cn.archko.pdf.activities

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * @author: archko 2021/10/4 :08:37
 */

data class PdfPreferences(
    val showExtension: Boolean = true,
    val fullscreen: Boolean = true,
    val autocrop: Boolean = true,
    val verticalScrollLock: Boolean = true,
    val sideMargins2: Int = 10,
    val topMargin: Int = 10,
    val keepOn: Boolean = false,
    val list_style: Int = 0,
    val dartTheme: Boolean = true,
    val orientation: Int = 7,
) {
    override fun toString(): String {
        return "PdfPreferences(showExtension=$showExtension, fullscreen=$fullscreen, autocrop=$autocrop, verticalScrollLock=$verticalScrollLock, sideMargins2=$sideMargins2, topMargin=$topMargin, keepOn=$keepOn, list_style=$list_style, dartTheme=$dartTheme, orientation=$orientation)"
    }
}

object PreferencesKeys {
    val PREF_SHOW_EXTENSION = booleanPreferencesKey("showExtension")

    val PREF_ORIENTATION = intPreferencesKey("orientation")
    val PREF_FULLSCREEN = booleanPreferencesKey("fullscreen")
    val PREF_AUTOCROP = booleanPreferencesKey("autocrop")
    val PREF_VERTICAL_SCROLL_LOCK = booleanPreferencesKey("verticalScrollLock")
    val PREF_SIDE_MARGINS = intPreferencesKey("sideMargins2") // sideMargins was boolean

    val PREF_TOP_MARGIN = intPreferencesKey("topMargin")
    val PREF_KEEP_ON = booleanPreferencesKey("keepOn")
    val PREF_LIST_STYLE = intPreferencesKey("list_style")
    val PREF_DART_THEME = booleanPreferencesKey("pref_dart_theme")
}

class PdfPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val TAG: String = "PdfPreferencesRepo"

    val pdfPreferencesFlow: Flow<PdfPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val showExtension = preferences[PreferencesKeys.PREF_SHOW_EXTENSION] ?: true
            val fullscreen = preferences[PreferencesKeys.PREF_FULLSCREEN] ?: true
            val autocrop = preferences[PreferencesKeys.PREF_AUTOCROP] ?: true
            val verticalScrollLock = preferences[PreferencesKeys.PREF_VERTICAL_SCROLL_LOCK] ?: true
            val sideMargins2 = preferences[PreferencesKeys.PREF_SIDE_MARGINS] ?: 0
            val topMargin = preferences[PreferencesKeys.PREF_TOP_MARGIN] ?: 0
            val keepOn = preferences[PreferencesKeys.PREF_KEEP_ON] ?: false
            val list_style = preferences[PreferencesKeys.PREF_LIST_STYLE] ?: 0
            val dartTheme = preferences[PreferencesKeys.PREF_DART_THEME] ?: false
            val orientation = preferences[PreferencesKeys.PREF_ORIENTATION] ?: 0
            PdfPreferences(
                showExtension = showExtension,
                fullscreen = fullscreen,
                autocrop = autocrop,
                verticalScrollLock = verticalScrollLock,
                sideMargins2 = sideMargins2,
                topMargin = topMargin,
                keepOn = keepOn,
                list_style = list_style,
                dartTheme = dartTheme,
                orientation = orientation,
            )
        }

    suspend fun setShowExtension(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_SHOW_EXTENSION] = enable
        }
    }

    suspend fun setFullscreen(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_FULLSCREEN] = enable
        }
    }

    suspend fun setAutocrop(autocrop: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_AUTOCROP] = autocrop
        }
    }

    suspend fun setVerticalScrollLock(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_VERTICAL_SCROLL_LOCK] = enable
        }
    }

    suspend fun setSideMargins2(sideMargins2: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_SIDE_MARGINS] = sideMargins2
        }
    }

    suspend fun setTopMargin(topMargin: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_TOP_MARGIN] = topMargin
        }
    }

    suspend fun setKeepOn(keepOn: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_KEEP_ON] = keepOn
        }
    }

    suspend fun setListStyle(list_style: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_LIST_STYLE] = list_style
        }
    }

    suspend fun setDartTheme(it: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_DART_THEME] = it
        }
    }

    suspend fun setOrientation(orientation: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREF_ORIENTATION] = orientation
        }
    }
}