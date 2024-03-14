package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * @author: archko 2024/3/14 :16:43
 */
fun LazyListScope.twoTargetIconButtonPreference(
    key: String,
    title: @Composable () -> Unit,
    iconButtonIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    iconButtonEnabled: Boolean = enabled,
    onIconButtonClick: () -> Unit
) {
    item(key = key, contentType = "TwoTargetIconButtonPreference") {
        TwoTargetIconButtonPreference(
            title = title,
            iconButtonIcon = iconButtonIcon,
            modifier = modifier,
            enabled = enabled,
            icon = icon,
            summary = summary,
            onClick = onClick,
            iconButtonEnabled = iconButtonEnabled,
            onIconButtonClick = onIconButtonClick
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TwoTargetIconButtonPreference(
    title: @Composable () -> Unit,
    iconButtonIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    iconButtonEnabled: Boolean = enabled,
    onIconButtonClick: () -> Unit
) {
    TwoTargetPreference(
        title = title,
        secondTarget = {
            val theme = LocalPreferenceTheme.current
            IconButton(
                onClick = onIconButtonClick,
                modifier =
                Modifier.padding(
                    theme.padding.copy(start = theme.horizontalSpacing).offset((-12).dp)
                ),
                enabled = iconButtonEnabled,
                colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = theme.iconColor,
                    disabledContentColor = theme.iconColor.copy(alpha = theme.disabledOpacity)
                ),
                content = iconButtonIcon
            )
        },
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        onClick = onClick
    )
}

@Composable
@Preview(showBackground = true)
private fun TwoTargetIconButtonPreferencePreview() {
    ProvidePreferenceTheme {
        TwoTargetIconButtonPreference(
            title = { Text(text = "Two target icon button preference") },
            iconButtonIcon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            summary = { Text(text = "Summary") }
        ) {}
    }
}