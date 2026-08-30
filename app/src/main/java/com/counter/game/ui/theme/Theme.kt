package com.counter.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = Black,
    outline = Black,
    error = Black,
    onError = White,
)

val LocalFontScale = staticCompositionLocalOf { 1.0f }

@Composable
fun MyApplicationTheme(
    fontScale: Float = LocalFontScale.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = Typography,
            content = content,
        )
    }
}