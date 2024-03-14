package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview

/**
 * @author: archko 2024/3/14 :16:19
 */
inline fun LazyListScope.checkboxPreference(
    key: String,
    defaultValue: Boolean,
    crossinline title: @Composable (Boolean) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<Boolean> = {
        rememberPreferenceState(key, defaultValue)
    },
    crossinline enabled: (Boolean) -> Boolean = { true },
    noinline icon: @Composable ((Boolean) -> Unit)? = null,
    noinline summary: @Composable ((Boolean) -> Unit)? = null
) {
    item(key = key, contentType = "CheckboxPreference") {
        val state = rememberState()
        val value by state
        CheckboxPreference(
            state = state,
            title = { title(value) },
            modifier = modifier,
            enabled = enabled(value),
            icon = icon?.let { { it(value) } },
            summary = summary?.let { { it(value) } }
        )
    }
}

@Composable
fun CheckboxPreference(
    state: MutableState<Boolean>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null
) {
    var value by state
    CheckboxPreference(
        value = value,
        onValueChange = { value = it },
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary
    )
}

@Composable
fun CheckboxPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null
) {
    Preference(
        title = title,
        modifier = modifier.toggleable(value, enabled, Role.Checkbox, onValueChange),
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            val theme = LocalPreferenceTheme.current
            Checkbox(
                checked = value,
                onCheckedChange = null,
                modifier = Modifier.padding(theme.padding.copy(start = theme.horizontalSpacing)),
                enabled = enabled
            )
        }
    )
}

@Composable
@Preview(showBackground = true)
private fun CheckboxPreferencePreview() {
    ProvidePreferenceTheme {
        CheckboxPreference(
            state = remember { mutableStateOf(true) },
            title = { Text(text = "Checkbox preference") },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            summary = { Text(text = "Summary") }
        )
    }
}