package cn.archko.pdf.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * @author: archko 2024/3/14 :16:18
 */
fun LazyListScope.basicPreference(
    key: String,
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    item(key = key, contentType = "BasicPreference") {
        BasicPreference(
            textContainer = textContainer,
            modifier = modifier,
            enabled = enabled,
            iconContainer = iconContainer,
            widgetContainer = widgetContainer,
            onClick = onClick
        )
    }
}

@Composable
fun BasicPreference(
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier =
        modifier.then(
            if (onClick != null) {
                Modifier.clickable(enabled, onClick = onClick)
            } else {
                Modifier
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconContainer()
        Box(modifier = Modifier.weight(1f)) { textContainer() }
        widgetContainer()
    }
}