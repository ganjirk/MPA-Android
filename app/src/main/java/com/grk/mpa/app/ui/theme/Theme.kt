package com.grk.mpa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MpaBlueDark,
    onPrimary = Color(0xFF102B65),
    primaryContainer = Color(0xFF20386D),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = MpaTealDark,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFB8F2E8),
    background = MpaDarkBackground,
    onBackground = MpaDarkOnBackground,
    surface = MpaDarkSurface,
    onSurface = MpaDarkOnBackground,
    onSurfaceVariant = MpaDarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = MpaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = MpaTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F2E8),
    onSecondaryContainer = Color(0xFF00201C),
    background = MpaBackground,
    onBackground = MpaOnBackground,
    surface = MpaSurface,
    onSurface = MpaOnBackground,
    onSurfaceVariant = MpaOnSurfaceVariant
)

@Composable
fun MPAAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep MPA branding consistent instead of using device-specific dynamic colors.
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
