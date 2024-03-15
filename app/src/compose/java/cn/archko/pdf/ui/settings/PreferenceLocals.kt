package cn.archko.pdf.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @author: archko 2024/3/14 :16:26
 */
@Composable
fun ProvidePreferenceLocals(
    flow: MutableStateFlow<Preferences> = defaultPreferenceFlow(),
    theme: PreferenceTheme = preferenceTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPreferenceFlow provides flow,
        LocalPreferenceTheme provides theme,
        content = content
    )
}

internal fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}