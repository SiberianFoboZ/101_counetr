package com.counter.game.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
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
}