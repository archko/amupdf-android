package cn.archko.pdf.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// dark palettes
private val DarkGreenColorPalette = darkColorScheme(
    primary = green200,
    primaryContainer = green700,
    secondary = teal200,
    secondaryContainer = green700,
    onSecondaryContainer = Color.Black,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    error = Color.Red,
)

private val DarkPurpleColorPalette = darkColorScheme(
    primary = purple200,
    primaryContainer = purple700,
    secondary = teal200,
    secondaryContainer = purple700,
    onSecondaryContainer = Color.White,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    error = Color.Red,
)

private val DarkBlueColorPalette = darkColorScheme(
    primary = blue200,
    primaryContainer = blue700,
    secondary = teal200,
    secondaryContainer = blue700,
    onSecondaryContainer = Color.White,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    error = Color.Red,
)

private val DarkOrangeColorPalette = darkColorScheme(
    primary = orange200,
    primaryContainer = orange700,
    secondary = teal200,
    secondaryContainer = orange700,
    onSecondaryContainer = Color.White,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,
    error = Color.Red,
)

// Light pallets
private val LightGreenColorPalette = lightColorScheme(
    primary = green500,
    primaryContainer = green700,
    secondary = teal200,
    secondaryContainer = green700,
    onSecondaryContainer = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black
)

private val LightPurpleColorPalette = lightColorScheme(
    primary = purple,
    primaryContainer = purple700,
    secondary = teal200,
    secondaryContainer = purple700,
    onSecondaryContainer = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
)

private val LightBlueColorPalette = lightColorScheme(
    primary = blue500,
    primaryContainer = blue700,
    secondary = teal200,
    secondaryContainer = blue700,
    onSecondaryContainer = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
)

private val LightOrangeColorPalette = lightColorScheme(
    primary = orange500,
    primaryContainer = orange700,
    secondary = teal200,
    secondaryContainer = orange700,
    onSecondaryContainer = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
)

enum class ColorPallet {
    GREEN, PURPLE, ORANGE, BLUE
}

@Composable
fun ComposeCookBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPallet: ColorPallet = ColorPallet.GREEN,
    content: @Composable() () -> Unit,
) {
    val colors = when (colorPallet) {
        ColorPallet.GREEN -> if (darkTheme) DarkGreenColorPalette else LightGreenColorPalette
        ColorPallet.PURPLE -> if (darkTheme) DarkPurpleColorPalette else LightPurpleColorPalette
        ColorPallet.ORANGE -> if (darkTheme) DarkOrangeColorPalette else LightOrangeColorPalette
        ColorPallet.BLUE -> if (darkTheme) DarkBlueColorPalette else LightBlueColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}