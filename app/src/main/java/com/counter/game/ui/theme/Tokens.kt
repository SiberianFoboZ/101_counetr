package com.counter.game.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цветовой токен — логическая группа в UI:
 *  - [TEXT] — основной цвет текста и активных иконок.
 *  - [BACKGROUND] — фон экранов и поверхностей.
 *  - [SURFACE] — поверхность карточек/диалогов.
 *  - [ON_SURFACE] — цвет текста, лежащего на [SURFACE].
 *  - [SURFACE_VARIANT] — приглушённый фон (баннеры, заголовки секций).
 *  - [OUTLINE] — рамки и разделители.
 */
enum class ThemeToken {
    TEXT,
    BACKGROUND,
    SURFACE,
    ON_SURFACE,
    SURFACE_VARIANT,
    OUTLINE,
}

/**
 * Именованные пресеты, которые показываются пользователю в экране «Тема».
 * Значение [id] сохраняется в БД; добавление нового пресета — только в конец списка,
 * чтобы не сломать сохранённые настройки у пользователей.
 */
enum class ThemePreset(val id: String, val displayName: String) {
    CLASSIC("classic", "Чёрно‑белая"),
    SEPIA("sepia", "Сепия"),
    OCEAN("ocean", "Океан"),
    FOREST("forest", "Лес"),
    CRIMSON("crimson", "Багровая"),
    CUSTOM("custom", "Своя"),
    ;

    companion object {
        fun fromId(id: String?): ThemePreset = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

/**
 * Набор токенов и способы их получения.
 *
 * В режиме пресета цвета вычисляются детерминированно из [preset].
 * В режиме CUSTOM используется [customTokens], заполненный из БД.
 */
data class ThemePalette(
    val preset: ThemePreset,
    val customTokens: Map<ThemeToken, Color> = emptyMap(),
) {
    fun color(token: ThemeToken): Color = when (preset) {
        ThemePreset.CUSTOM -> customTokens[token] ?: presetColors(ThemePreset.CLASSIC)[token]!!
        else -> presetColors(preset)[token]!!
    }
}

/**
 * Жёстко зашитые цвета пресетов. Каждый пресет задаёт 6 токенов.
 * Тёмные тона не используются — приложение намеренно остаётся в светлой палитре
 * (`dynamicColor = false`, `darkTheme` выключен).
 */
fun presetColors(preset: ThemePreset): Map<ThemeToken, Color> = when (preset) {
    ThemePreset.CLASSIC -> mapOf(
        ThemeToken.TEXT to Color(0xFF000000),
        ThemeToken.BACKGROUND to Color(0xFFFFFFFF),
        ThemeToken.SURFACE to Color(0xFFFFFFFF),
        ThemeToken.ON_SURFACE to Color(0xFF000000),
        ThemeToken.SURFACE_VARIANT to Color(0xFFF5F5F5),
        ThemeToken.OUTLINE to Color(0xFFCCCCCC),
    )
    ThemePreset.SEPIA -> mapOf(
        ThemeToken.TEXT to Color(0xFF4A3520),
        ThemeToken.BACKGROUND to Color(0xFFF5EBD6),
        ThemeToken.SURFACE to Color(0xFFFFF7E6),
        ThemeToken.ON_SURFACE to Color(0xFF4A3520),
        ThemeToken.SURFACE_VARIANT to Color(0xFFEBDFC4),
        ThemeToken.OUTLINE to Color(0xFFB8A684),
    )
    ThemePreset.OCEAN -> mapOf(
        ThemeToken.TEXT to Color(0xFF0A2540),
        ThemeToken.BACKGROUND to Color(0xFFF1F6FB),
        ThemeToken.SURFACE to Color(0xFFFFFFFF),
        ThemeToken.ON_SURFACE to Color(0xFF0A2540),
        ThemeToken.SURFACE_VARIANT to Color(0xFFE3ECF5),
        ThemeToken.OUTLINE to Color(0xFF9BB4CC),
    )
    ThemePreset.FOREST -> mapOf(
        ThemeToken.TEXT to Color(0xFF1F3A20),
        ThemeToken.BACKGROUND to Color(0xFFEFF5E8),
        ThemeToken.SURFACE to Color(0xFFFFFFFF),
        ThemeToken.ON_SURFACE to Color(0xFF1F3A20),
        ThemeToken.SURFACE_VARIANT to Color(0xFFD9E5CC),
        ThemeToken.OUTLINE to Color(0xFF95B188),
    )
    ThemePreset.CRIMSON -> mapOf(
        ThemeToken.TEXT to Color(0xFF4A0E14),
        ThemeToken.BACKGROUND to Color(0xFFFBECEC),
        ThemeToken.SURFACE to Color(0xFFFFFFFF),
        ThemeToken.ON_SURFACE to Color(0xFF4A0E14),
        ThemeToken.SURFACE_VARIANT to Color(0xFFE9D2D2),
        ThemeToken.OUTLINE to Color(0xFFB98888),
    )
    ThemePreset.CUSTOM -> emptyMap()
}

/**
 * Утилита: парсит строку `#RRGGBB` или `#AARRGGBB` в [Color].
 * Возвращает [fallback] при ошибке.
 */
fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        val cleaned = hex.removePrefix("#")
        val parsed = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

fun Color.toHexString(): String {
    val argb = this.toArgb()
    return "#%08X".format(argb.toLong() and 0xFFFFFFFFL)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)