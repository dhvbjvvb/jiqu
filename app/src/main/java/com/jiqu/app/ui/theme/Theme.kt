package com.jiqu.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val JinanColorScheme = lightColorScheme(
    primary = JinanBlueDark,
    onPrimary = JinanSurface,
    primaryContainer = JinanBlue,
    onPrimaryContainer = JinanSurface,
    secondary = Color(0xFF00677D),
    onSecondary = JinanSurface,
    secondaryContainer = JinanCyanSoft,
    onSecondaryContainer = Color(0xFF005C70),
    background = JinanBackground,
    onBackground = JinanText,
    surface = JinanSurface,
    onSurface = JinanText,
    surfaceVariant = JinanSurfaceVariant,
    onSurfaceVariant = JinanTextSecondary,
    outline = JinanOutline
)

private val JinanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF75D0FF),
    onPrimary = Color(0xFF00344E),
    primaryContainer = Color(0xFF004D72),
    onPrimaryContainer = Color(0xFFD1ECFF),
    secondary = Color(0xFF78D8F2),
    onSecondary = Color(0xFF003641),
    secondaryContainer = Color(0xFF004E5E),
    onSecondaryContainer = Color(0xFFAEEBFF),
    background = JinanDarkBackground,
    onBackground = JinanDarkText,
    surface = JinanDarkSurface,
    onSurface = JinanDarkText,
    surfaceVariant = JinanDarkSurfaceVariant,
    onSurfaceVariant = JinanDarkTextSecondary,
    outline = JinanDarkOutline
)

@Composable
fun 即取Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDarkTheme) JinanDarkColorScheme else JinanColorScheme,
        typography = Typography,
        content = content
    )
}
