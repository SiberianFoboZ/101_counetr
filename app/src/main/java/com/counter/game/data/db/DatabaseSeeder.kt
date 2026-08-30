package com.counter.game.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.counter.game.data.entity.SettingsEntity

object DatabaseSeeder {
    suspend fun seed(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            seedCardDefinitions(db)
            seedSettings(db)
            seedRules(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun seedCardDefinitions(db: SupportSQLiteDatabase) {
        CardDefinitionsSeed.rows.forEach { row ->
            db.execSQL(
                "INSERT OR IGNORE INTO card_definitions(code, label, base_value, is_face) VALUES (?, ?, ?, ?)",
                arrayOf(row.code, row.label, row.baseValue, if (row.isFace) 1 else 0),
            )
        }
    }

    private fun seedSettings(db: SupportSQLiteDatabase) {
        val s = SettingsEntity()
        db.execSQL(
            "INSERT OR REPLACE INTO settings(id, queen_value, queen_spades_value, king_value, threshold_score, font_scale, theme_accent) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                s.id, s.queenValue, s.queenSpadesValue, s.kingValue,
                s.thresholdScore, s.fontScale.toDouble(), s.themeAccent,
            ),
        )
    }

    private fun seedRules(db: SupportSQLiteDatabase) {
        RulesSeed.rows.forEach { row ->
            db.execSQL(
                "INSERT OR IGNORE INTO rules(name, applies_to_card, priority, enabled, definition_json, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(
                    row.name, row.appliesToCard, row.priority,
                    if (row.enabled) 1 else 0, row.definitionJson, System.currentTimeMillis(),
                ),
            )
        }
    }

    internal object CardDefinitionsSeed {
        data class Row(val code: String, val label: String, val baseValue: Int, val isFace: Boolean)

        val rows: List<Row> = listOf(
            Row("6", "6", 6, false),
            Row("7", "7", 7, false),
            Row("8", "8", 8, false),
            Row("9", "9", 0, false),
            Row("10", "10", 10, false),
            Row("J", "Валет", 2, true),
            Row("A", "Туз", 11, true),
            Row("Q_hearts", "Дама ♥", 3, true),
            Row("Q_diamonds", "Дама ♦", 3, true),
            Row("Q_clubs", "Дама ♣", 3, true),
            Row("Q_spades", "Дама ♠", 3, true),
            Row("K_hearts", "Король ♥", 4, true),
            Row("K_diamonds", "Король ♦", 4, true),
            Row("K_clubs", "Король ♣", 4, true),
            Row("K_spades", "Король ♠", 4, true),
            // Синтетическая «карта» для FINAL_ADJUSTMENT правил (обнуление при 101 и т.п.).
            // Используется как applies_to_card у таких правил, чтобы удовлетворить FK constraint.
            Row("_final", "—", 0, false),
        )
    }

    internal object RulesSeed {
        data class Row(
            val name: String,
            val appliesToCard: String,
            val priority: Int,
            val enabled: Boolean,
            val definitionJson: String,
        )

        private const val BASE_VALUE = """{"type":"base_value"}"""
        private const val SETTING_QUEEN = """{"type":"setting","key":"queen_value"}"""
        private const val CONST_25 = """{"type":"const","value":25}"""

        val rows: List<Row> = listOf(
            Row(
                name = "Король пик",
                appliesToCard = "K_spades",
                priority = 100,
                enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {
                    "nominal": {"op":"==","value":4},
                    "suit": "spades",
                    "condition": {"op":">=","left":"card_count","right":{"type":"const","value":2}}
                  },
                  "then": {"type":"const","value":50},
                  "else": {"type":"const","value":4}
                }
                """.trimIndent(),
            ),
            Row(
                name = "Дама пик",
                appliesToCard = "Q_spades",
                priority = 90,
                enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {"nominal":{"op":"any"},"suit":"spades"},
                  "then": $CONST_25,
                  "else": $CONST_25
                }
                """.trimIndent(),
            ),
            Row(
                name = "Дама",
                appliesToCard = "Q_hearts",
                priority = 50,
                enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {"nominal":{"op":"any"},"suit":"non_spades"},
                  "then": $SETTING_QUEEN,
                  "else": $SETTING_QUEEN
                }
                """.trimIndent(),
            ),
            Row(
                name = "Базовая: 6", appliesToCard = "6", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: 7", appliesToCard = "7", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: 8", appliesToCard = "8", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: 9", appliesToCard = "9", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: 10", appliesToCard = "10", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: Валет", appliesToCard = "J", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
            Row(
                name = "Базовая: Туз", appliesToCard = "A", priority = 10, enabled = true,
                definitionJson = baseRuleJson(),
            ),
        )

        private fun baseRuleJson(): String = """
            {
              "version": 2,
              "match": {"nominal":{"op":"any"},"suit":"any"},
              "then": $BASE_VALUE,
              "else": $BASE_VALUE
            }
        """.trimIndent()
    }
}