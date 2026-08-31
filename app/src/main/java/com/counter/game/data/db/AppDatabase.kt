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
    version = 3,
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
    }
}