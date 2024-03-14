package cn.archko.pdf.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * @author: archko 2024/3/14 :16:44
 */
fun LazyListScope.twoTargetPreference(
    key: String,
    title: @Composable () -> Unit,
    secondTarget: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    item(key = key, contentType = "TwoTargetPreference") {
        TwoTargetPreference(
            title = title,
            secondTarget = secondTarget,
            modifier = modifier,
            enabled = enabled,
            icon = icon,
            summary = summary,
            onClick = onClick
        )
    }
}

@Composable
fun TwoTargetPreference(
    title: @Composable () -> Unit,
    secondTarget: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            val theme = LocalPreferenceTheme.current
            Row(
                modifier = Modifier.padding(start = theme.horizontalSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(DividerDefaults.Thickness, theme.dividerHeight)
                        .background(
                            DividerDefaults.color.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            }
                        )
                )
                secondTarget()
            }
        },
        onClick = onClick
    )
}