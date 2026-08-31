package com.counter.game.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "queen_value") val queenValue: Int = 3,
    @ColumnInfo(name = "queen_spades_value") val queenSpadesValue: Int = 25,
    @ColumnInfo(name = "king_value") val kingValue: Int = 4,
    @ColumnInfo(name = "threshold_score") val thresholdScore: Int = 101,
    @ColumnInfo(name = "font_scale") val fontScale: Float = 1.0f,
    @ColumnInfo(name = "theme_accent") val themeAccent: String = "default",
    @ColumnInfo(name = "theme_preset", defaultValue = "classic")
    val themePreset: String = "classic",
    @ColumnInfo(name = "theme_token_text") val themeTokenText: String? = null,
    @ColumnInfo(name = "theme_token_background") val themeTokenBackground: String? = null,
    @ColumnInfo(name = "theme_token_surface") val themeTokenSurface: String? = null,
    @ColumnInfo(name = "theme_token_on_surface") val themeTokenOnSurface: String? = null,
    @ColumnInfo(name = "theme_token_surface_variant") val themeTokenSurfaceVariant: String? = null,
    @ColumnInfo(name = "theme_token_outline") val themeTokenOutline: String? = null,
)