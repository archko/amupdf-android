package cn.archko.pdf.ui.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * @author: archko 2024/3/14 :16:32
 */
@OptIn(DelicateCoroutinesApi::class)
fun SharedPreferences.getPreferenceFlow(): MutableStateFlow<Preferences> =
    MutableStateFlow(preferences).also {
        GlobalScope.launch { it.drop(1).collect { preferences = it } }
    }

private var SharedPreferences.preferences: Preferences
    get() = @Suppress("UNCHECKED_CAST") MapPreferences(all as Map<String, Any>)
    set(value) {
        edit {
            clear()
            value.asMap().forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") putStringSet(key, value as Set<String>)
                    else -> throw IllegalArgumentException("Unsupported type for value $value")
                }
            }
        }
    }
