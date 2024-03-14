package cn.archko.pdf.ui.settings


import android.preference.PreferenceManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @author: archko 2024/3/14 :16:21
 */
@Composable
fun defaultPreferenceFlow(): MutableStateFlow<Preferences> {
    val view = LocalView.current
    return if (view.isInEditMode) {
        MutableStateFlow(MapPreferences())
    } else {
        val context = LocalContext.current
        @Suppress("DEPRECATION")
        PreferenceManager.getDefaultSharedPreferences(context).getPreferenceFlow()
    }
}

val LocalPreferenceFlow =
    compositionLocalOf<MutableStateFlow<Preferences>> { noLocalProvidedFor("LocalPreferenceFlow") }

@Composable
fun ProvidePreferenceFlow(
    flow: MutableStateFlow<Preferences> = defaultPreferenceFlow(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPreferenceFlow provides flow, content = content)
}