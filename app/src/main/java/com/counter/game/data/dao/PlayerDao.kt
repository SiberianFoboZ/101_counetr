package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.counter.game.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE is_archived = 0 ORDER BY created_at ASC")
    fun observeActive(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE is_archived = 0 ORDER BY created_at ASC")
    suspend fun listActive(): List<PlayerEntity>

    @Query("SELECT COUNT(*) FROM players WHERE is_archived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun byId(id: Long): PlayerEntity?

    @Insert
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Query("UPDATE players SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("SELECT IFNULL(MAX(CAST(SUBSTR(name, 7) AS INTEGER)), 0) FROM players WHERE name LIKE 'Игрок %'")
    suspend fun maxDefaultNumber(): Int
}