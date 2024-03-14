package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * @author: archko 2024/3/14 :16:31
 */
fun LazyListScope.footerPreference(
    key: String,
    summary: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icon: @Composable () -> Unit = FooterPreferenceDefaults.Icon
) {
    item(key = key, contentType = "FooterPreference") {
        FooterPreference(summary = summary, modifier = modifier, icon = icon)
    }
}

@Composable
fun FooterPreference(
    summary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = FooterPreferenceDefaults.Icon
) {
    Preference(
        title = {
            val theme = LocalPreferenceTheme.current
            Box(modifier = Modifier.padding(bottom = theme.verticalSpacing)) {
                CompositionLocalProvider(LocalContentColor provides theme.iconColor, content = icon)
            }
        },
        modifier = modifier,
        summary = summary,
    )
}

private object FooterPreferenceDefaults {
    val Icon: @Composable () -> Unit = {
        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
    }
}

@Composable
@Preview(showBackground = true)
private fun FooterPreferencePreview() {
    ProvidePreferenceTheme {
        FooterPreference(
            modifier = Modifier.fillMaxWidth(),
            summary = { Text(text = "Footer preference summary") }
        )
    }
}
