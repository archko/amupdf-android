package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * @author: archko 2024/3/14 :16:26
 */
fun LazyListScope.preferenceCategory(
    key: String,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    item(key = key, contentType = "PreferenceCategory") {
        PreferenceCategory(title = title, modifier = modifier)
    }
}

@Composable
fun PreferenceCategory(title: @Composable () -> Unit, modifier: Modifier = Modifier) {
    BasicPreference(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Box(
                modifier = Modifier.padding(theme.categoryPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                CompositionLocalProvider(LocalContentColor provides theme.categoryColor) {
                    ProvideTextStyle(value = theme.categoryTextStyle, content = title)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
@Preview(showBackground = true)
private fun PreferenceCategoryPreview() {
    ProvidePreferenceTheme {
        PreferenceCategory(
            title = { Text(text = "Preference category") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}