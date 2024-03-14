package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview

/**
 * @author: archko 2024/3/14 :16:27
 */
fun LazyListScope.radioButtonPreference(
    key: String,
    selected: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    item(key = key, contentType = "RadioButtonPreference") {
        RadioButtonPreference(
            selected = selected,
            title = title,
            modifier = modifier,
            enabled = enabled,
            summary = summary,
            widgetContainer = widgetContainer,
            onClick = onClick
        )
    }
}

@Composable
fun RadioButtonPreference(
    selected: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Preference(
        title = title,
        modifier = modifier.selectable(selected, enabled, Role.RadioButton, onClick),
        enabled = enabled,
        icon = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        summary = summary,
        widgetContainer = widgetContainer
    )
}

@Composable
@Preview(showBackground = true)
private fun RadioButtonPreferencePreview() {
    ProvidePreferenceTheme {
        RadioButtonPreference(
            selected = true,
            title = { Text(text = "Radio button preference") },
            modifier = Modifier.fillMaxWidth(),
            summary = { Text(text = "Summary") }
        ) {}
    }
}
