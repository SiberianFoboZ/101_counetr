package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.counter.game.data.entity.CardDefinitionEntity

@Dao
interface CardDefinitionDao {
    @Query("SELECT * FROM card_definitions ORDER BY id ASC")
    suspend fun listAll(): List<CardDefinitionEntity>

    @Query("SELECT * FROM card_definitions WHERE code = :code LIMIT 1")
    suspend fun byCode(code: String): CardDefinitionEntity?

    @Query("SELECT COUNT(*) FROM card_definitions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(definitions: List<CardDefinitionEntity>)
}