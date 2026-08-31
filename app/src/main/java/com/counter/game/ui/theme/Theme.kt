package com.counter.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalFontScale = staticCompositionLocalOf { 1.0f }
val LocalThemePalette = staticCompositionLocalOf<ThemePalette> {
    ThemePalette(ThemePreset.CLASSIC)
}

/**
 * Строит Material3 ColorScheme из [palette]. Используется только [lightColorScheme],
 * потому что приложение намеренно остаётся в светлой теме (см. QWEN.md).
 */
private fun buildLightScheme(palette: ThemePalette): androidx.compose.material3.ColorScheme {
    val text = palette.color(ThemeToken.TEXT)
    val background = palette.color(ThemeToken.BACKGROUND)
    val surface = palette.color(ThemeToken.SURFACE)
    val onSurface = palette.color(ThemeToken.ON_SURFACE)
    val surfaceVariant = palette.color(ThemeToken.SURFACE_VARIANT)
    val outline = palette.color(ThemeToken.OUTLINE)
    return lightColorScheme(
        primary = text,
        onPrimary = background,
        secondary = text,
        onSecondary = background,
        tertiary = text,
        onTertiary = background,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurface,
        outline = outline,
        outlineVariant = outline,
        error = text,
        onError = background,
    )
}

@Composable
fun MyApplicationTheme(
    palette: ThemePalette = LocalThemePalette.current,
    fontScale: Float = LocalFontScale.current,
    content: @Composable () -> Unit,
) {
    val scheme = buildLightScheme(palette)
    CompositionLocalProvider(
        LocalFontScale provides fontScale,
        LocalThemePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            content = content,
        )
    }
}