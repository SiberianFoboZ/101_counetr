package com.counter.game.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.counter.game.data.dao.CardDefinitionDao
import com.counter.game.data.dao.GameDao
import com.counter.game.data.dao.GamePlayerDao
import com.counter.game.data.dao.PlayerDao
import com.counter.game.data.dao.RuleDao
import com.counter.game.data.dao.RoundDao
import com.counter.game.data.dao.RoundEntryDao
import com.counter.game.data.dao.SettingsDao
import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.data.entity.GameEntity
import com.counter.game.data.entity.GamePlayerEntity
import com.counter.game.data.entity.PlayerEntity
import com.counter.game.data.entity.RuleEntity
import com.counter.game.data.entity.RoundCardEntity
import com.counter.game.data.entity.RoundEntryEntity
import com.counter.game.data.entity.RoundEntity
import com.counter.game.data.entity.SettingsEntity

@Database(
    entities = [
        PlayerEntity::class,
        CardDefinitionEntity::class,
        SettingsEntity::class,
        RuleEntity::class,
        GameEntity::class,
        GamePlayerEntity::class,
        RoundEntity::class,
        RoundEntryEntity::class,
        RoundCardEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun cardDefinitionDao(): CardDefinitionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun ruleDao(): RuleDao
    abstract fun gameDao(): GameDao
    abstract fun gamePlayerDao(): GamePlayerDao
    abstract fun roundDao(): RoundDao
    abstract fun roundEntryDao(): RoundEntryDao

    companion object {
        /**
         * v1 → v2: добавляем синтетическую «карту» `_final` для FINAL_ADJUSTMENT правил
         * (обнуление при 101 и т.п.). У таких правил `applies_to_card` указывает на эту
         * запись, чтобы удовлетворить FK constraint.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO card_definitions(code, label, base_value, is_face) VALUES (?, ?, ?, ?)",
                    arrayOf("_final", "—", 0, 0),
                )
            }
        }

        /**
         * v2 → v3: добавляем поля цветовой палитры в `settings`.
         * Для уже установленных приложений все столбцы получают NULL/дефолт,
         * что соответствует классической чёрно-белой теме.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_preset TEXT NOT NULL DEFAULT 'classic'")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_text TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_background TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_surface TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_on_surface TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_surface_variant TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN theme_token_outline TEXT")
            }
        }

        /**
         * v3 → v4: переустановка seed-правил под обновлённый набор (дамы/короли/обнуление при 101).
         * Пользовательские правила с именами, не входящими в новый список, остаются нетронутыми.
         * Это компромисс: точечная правка seed без полного wipe.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    rule3to4Names().forEach { name ->
                        db.execSQL("DELETE FROM rules WHERE name = ?", arrayOf(name))
                    }
                    val now = System.currentTimeMillis()
                    rule3to4Rows().forEach { r ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO rules(name, applies_to_card, priority, enabled, definition_json, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(r.name, r.appliesToCard, r.priority, if (r.enabled) 1 else 0, r.definitionJson, now),
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }

        /**
         * v4 → v5: повторная переустановка seed-правил.
         * MIGRATION_3_4 уже содержит правильный JSON, но у пользователей, которые обновились
         * с более ранней сборки (где миграция была с `card_count == 1` и `setting:queen_value`),
         * в БД остались старые определения — Room миграцию повторно не запускает. Этот шаг
         * гарантирует, что итоговый набор правил соответствует текущему [DatabaseSeeder].
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    rule3to4Names().forEach { name ->
                        db.execSQL("DELETE FROM rules WHERE name = ?", arrayOf(name))
                    }
                    val now = System.currentTimeMillis()
                    rule3to4Rows().forEach { r ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO rules(name, applies_to_card, priority, enabled, definition_json, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(r.name, r.appliesToCard, r.priority, if (r.enabled) 1 else 0, r.definitionJson, now),
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }

        private fun rule3to4Names(): List<String> = listOf(
            "Дама",
            "Дама пик",
            "Король",
            "Король пик",
            "Базовая: 6",
            "Базовая: 7",
            "Базовая: 8",
            "Базовая: 9",
            "Базовая: 10",
            "Базовая: Валет",
            "Базовая: Туз",
            "Обнуление при 101",
        )

        private data class RuleSeedRow(
            val name: String,
            val appliesToCard: String,
            val priority: Int,
            val enabled: Boolean,
            val definitionJson: String,
        )

        private const val R34_BASE_VALUE = """{"type":"base_value"}"""
        private const val R34_SETTING_QUEEN = """{"type":"setting","key":"queen_value"}"""
        private const val R34_CONST_40 = """{"type":"const","value":40}"""
        private const val R34_CONST_50 = """{"type":"const","value":50}"""
        private const val R34_CONST_NEG_101 = """{"type":"const","value":-101}"""

        private fun r34BaseRuleJson(): String = """
            {
              "version": 2,
              "match": {"nominal":{"op":"any"},"suit":"any"},
              "then": $R34_BASE_VALUE,
              "else": $R34_BASE_VALUE
            }
        """.trimIndent()

        private fun rule3to4Rows(): List<RuleSeedRow> = listOf(
            RuleSeedRow(
                name = "Дама", appliesToCard = "Q_hearts", priority = 50, enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {"nominal":{"op":"any"},"suit":"non_spades"},
                  "then": $R34_BASE_VALUE,
                  "else": $R34_BASE_VALUE
                }
                """.trimIndent(),
            ),
            RuleSeedRow(
                name = "Дама пик", appliesToCard = "Q_spades", priority = 90, enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {
                    "nominal": {"op":"any"},
                    "suit": "spades",
                    "condition": {"op":"==","left":"distinct_card_codes","right":{"type":"const","value":1}}
                  },
                  "then": $R34_CONST_40,
                  "else": $R34_BASE_VALUE
                }
                """.trimIndent(),
            ),
            RuleSeedRow(
                name = "Король", appliesToCard = "K_hearts", priority = 30, enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {"nominal":{"op":"any"},"suit":"non_spades"},
                  "then": $R34_BASE_VALUE,
                  "else": $R34_BASE_VALUE
                }
                """.trimIndent(),
            ),
            RuleSeedRow(
                name = "Король пик", appliesToCard = "K_spades", priority = 95, enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "match": {
                    "nominal": {"op":"any"},
                    "suit": "spades",
                    "condition": {"op":"==","left":"distinct_card_codes","right":{"type":"const","value":1}}
                  },
                  "then": $R34_CONST_50,
                  "else": $R34_BASE_VALUE
                }
                """.trimIndent(),
            ),
            RuleSeedRow("Базовая: 6", "6", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: 7", "7", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: 8", "8", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: 9", "9", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: 10", "10", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: Валет", "J", 10, true, r34BaseRuleJson()),
            RuleSeedRow("Базовая: Туз", "A", 10, true, r34BaseRuleJson()),
            RuleSeedRow(
                name = "Обнуление при 101", appliesToCard = "_final", priority = 1000, enabled = true,
                definitionJson = """
                {
                  "version": 2,
                  "kind": "final_adjustment",
                  "match": {
                    "nominal": {"op":"any"},
                    "suit": "any",
                    "condition": {"op":"==","left":"total_score","right":{"type":"const","value":101}}
                  },
                  "then": $R34_CONST_NEG_101,
                  "else": $R34_BASE_VALUE
                }
                """.trimIndent(),
            ),
        )
    }
}