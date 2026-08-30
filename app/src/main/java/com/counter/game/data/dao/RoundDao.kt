package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.counter.game.data.entity.RoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE game_id = :gameId ORDER BY round_number ASC")
    fun observeByGame(gameId: Long): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds WHERE game_id = :gameId ORDER BY round_number ASC")
    suspend fun listByGame(gameId: Long): List<RoundEntity>

    @Query("SELECT COALESCE(MAX(round_number), 0) FROM rounds WHERE game_id = :gameId")
    suspend fun maxRoundNumber(gameId: Long): Int

    @Insert
    suspend fun insert(round: RoundEntity): Long
}