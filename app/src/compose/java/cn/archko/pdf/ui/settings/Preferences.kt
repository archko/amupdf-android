package cn.archko.pdf.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @author: archko 2024/3/14 :16:21
 */

interface Preferences {
    operator fun <T> get(key: String): T?

    fun asMap(): Map<String, Any>

    fun toPreferences(): Preferences = toMutablePreferences()

    fun toMutablePreferences(): MutablePreferences
}

internal class MapPreferences(private val map: Map<String, Any> = emptyMap()) : Preferences {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String): T? = map[key] as T?

    override fun asMap(): Map<String, Any> = map

    override fun toMutablePreferences(): MutablePreferences =
        MapMutablePreferences(map.toMutableMap())
}

interface MutablePreferences : Preferences {
    operator fun <T> set(key: String, value: T?)

    fun remove(key: String) {
        set(key, null)
    }

    operator fun minusAssign(key: String) {
        remove(key)
    }

    fun clear()
}

internal class MapMutablePreferences(private val map: MutableMap<String, Any> = mutableMapOf()) :
    MutablePreferences {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String): T? = map[key] as T?

    override fun asMap(): Map<String, Any> = map

    override fun toMutablePreferences(): MutablePreferences =
        MapMutablePreferences(map.toMutableMap())

    override fun <T> set(key: String, value: T?) {
        if (value != null) {
            map[key] = value
        } else {
            map -= key
        }
    }

    override fun clear() {
        map.clear()
    }
}

fun LazyListScope.preference(
    key: String,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    item(key = key, contentType = "Preference") {
        Preference(
            title = title,
            modifier = modifier,
            enabled = enabled,
            icon = icon,
            summary = summary,
            widgetContainer = widgetContainer,
            onClick = onClick
        )
    }
}

@Composable
fun Preference(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    BasicPreference(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Column(
                modifier =
                Modifier.padding(
                    theme.padding.copy(
                        start = if (icon != null) 0.dp else Dp.Unspecified,
                        end = if (widgetContainer != null) 0.dp else Dp.Unspecified,
                    )
                )
            ) {
                PreferenceDefaults.TitleContainer(title = title, enabled = enabled)
                PreferenceDefaults.SummaryContainer(summary = summary, enabled = enabled)
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = { PreferenceDefaults.IconContainer(icon = icon, enabled = enabled) },
        widgetContainer = { widgetContainer?.invoke() },
        onClick = onClick
    )
}

internal object PreferenceDefaults {
    @Composable
    fun IconContainer(
        icon: @Composable (() -> Unit)?,
        enabled: Boolean,
        excludedEndPadding: Dp = 0.dp
    ) {
        if (icon != null) {
            val theme = LocalPreferenceTheme.current
            Box(
                modifier =
                Modifier
                    .widthIn(min = theme.iconContainerMinWidth - excludedEndPadding)
                    .padding(theme.padding.copy(end = 0.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                    content = icon
                )
            }
        }
    }

    @Composable
    fun TitleContainer(title: @Composable () -> Unit, enabled: Boolean) {
        val theme = LocalPreferenceTheme.current
        CompositionLocalProvider(
            LocalContentColor provides
                    theme.titleColor.let { if (enabled) it else it.copy(alpha = theme.disabledOpacity) }
        ) {
            ProvideTextStyle(value = theme.titleTextStyle, content = title)
        }
    }

    @Composable
    fun SummaryContainer(summary: (@Composable () -> Unit)?, enabled: Boolean) {
        if (summary != null) {
            val theme = LocalPreferenceTheme.current
            CompositionLocalProvider(
                LocalContentColor provides
                        theme.summaryColor.let {
                            if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                        }
            ) {
                ProvideTextStyle(value = theme.summaryTextStyle, content = summary)
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun PreferencePreview() {
    ProvidePreferenceTheme {
        Preference(
            title = { Text(text = "Preference") },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            summary = { Text(text = "Summary") }
        )
    }
}