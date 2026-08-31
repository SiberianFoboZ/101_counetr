package com.counter.game.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.ui.theme.ThemePalette
import com.counter.game.ui.theme.ThemePreset
import com.counter.game.ui.theme.ThemeToken
import com.counter.game.ui.theme.parseHexColor
import com.counter.game.ui.theme.presetColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeUi(
    val preset: ThemePreset = ThemePreset.CLASSIC,
    val tokens: Map<ThemeToken, Color> = presetColors(ThemePreset.CLASSIC),
)

class ThemeViewModel(private val container: AppContainer) : ViewModel() {

    private val pendingPreset = MutableStateFlow<ThemePreset?>(null)
    private val pendingTokens = MutableStateFlow<Map<ThemeToken, Color>?>(null)

    val state: StateFlow<ThemeUi> = combine(
        container.settingsRepository.observe(),
        pendingPreset,
        pendingTokens,
    ) { settings, presetOverride, tokensOverride ->
        val preset = presetOverride ?: ThemePreset.fromId(settings?.themePreset)
        val tokens = if (preset == ThemePreset.CUSTOM) {
            val fallback = presetColors(ThemePreset.CLASSIC)
            tokensOverride ?: buildCustomTokensFromSettings(settings, fallback)
        } else {
            presetColors(preset)
        }
        ThemeUi(preset = preset, tokens = tokens)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeUi())

    fun selectPreset(preset: ThemePreset) {
        pendingPreset.value = preset
        if (preset != ThemePreset.CUSTOM) {
            // При выборе встроенного пресета — сбрасываем ожидающие кастомные токены
            // и сразу сохраняем в БД (иначе при CUSTOM→пресет→CUSTOM теряются правки).
            pendingTokens.value = null
            save(preset = preset, tokens = presetColors(preset))
        }
    }

    fun updateToken(token: ThemeToken, color: Color) {
        val current = pendingTokens.value ?: state.value.tokens
        pendingTokens.value = current + (token to color)
        val preset = pendingPreset.value ?: state.value.preset
        save(preset = preset, tokens = pendingTokens.value!!)
    }

    fun resetCustomToDefaults() {
        val defaults = presetColors(ThemePreset.CLASSIC)
        pendingPreset.value = ThemePreset.CUSTOM
        pendingTokens.value = defaults
        save(preset = ThemePreset.CUSTOM, tokens = defaults)
    }

    private fun save(preset: ThemePreset, tokens: Map<ThemeToken, Color>) {
        viewModelScope.launch {
            val map = tokens.mapKeys { entry ->
                when (entry.key) {
                    ThemeToken.TEXT -> "text"
                    ThemeToken.BACKGROUND -> "background"
                    ThemeToken.SURFACE -> "surface"
                    ThemeToken.ON_SURFACE -> "on_surface"
                    ThemeToken.SURFACE_VARIANT -> "surface_variant"
                    ThemeToken.OUTLINE -> "outline"
                }
            }.mapValues { it.value.toHex() }
            container.settingsRepository.updateTheme(preset.id, map)
        }
    }

    private fun buildCustomTokensFromSettings(
        settings: com.counter.game.data.entity.SettingsEntity?,
        fallback: Map<ThemeToken, Color>,
    ): Map<ThemeToken, Color> {
        if (settings == null) return fallback
        return mapOf(
            ThemeToken.TEXT to parseHexColor(settings.themeTokenText, fallback[ThemeToken.TEXT]!!),
            ThemeToken.BACKGROUND to parseHexColor(settings.themeTokenBackground, fallback[ThemeToken.BACKGROUND]!!),
            ThemeToken.SURFACE to parseHexColor(settings.themeTokenSurface, fallback[ThemeToken.SURFACE]!!),
            ThemeToken.ON_SURFACE to parseHexColor(settings.themeTokenOnSurface, fallback[ThemeToken.ON_SURFACE]!!),
            ThemeToken.SURFACE_VARIANT to parseHexColor(settings.themeTokenSurfaceVariant, fallback[ThemeToken.SURFACE_VARIANT]!!),
            ThemeToken.OUTLINE to parseHexColor(settings.themeTokenOutline, fallback[ThemeToken.OUTLINE]!!),
        )
    }

    private fun Color.toHex(): String {
        val a = (alpha * 255).toInt() and 0xFF
        val r = (red * 255).toInt() and 0xFF
        val g = (green * 255).toInt() and 0xFF
        val b = (blue * 255).toInt() and 0xFF
        return "#%02X%02X%02X%02X".format(a, r, g, b)
    }

    /** Снимок текущей палитры — для пробрасывания в MyApplicationTheme. */
    fun snapshotPalette(): ThemePalette {
        val s = state.value
        return ThemePalette(preset = s.preset, customTokens = s.tokens)
    }
}