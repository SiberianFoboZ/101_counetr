package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.counter.game.data.entity.GameEntity
import com.counter.game.data.entity.GameStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observe(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = :status ORDER BY started_at DESC")
    fun observeByStatus(status: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = :status ORDER BY started_at DESC LIMIT 1")
    suspend fun latestByStatus(status: String): GameEntity?

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE games SET status = :status, finished_at = :finishedAt, winner_player_id = :winnerId WHERE id = :id")
    suspend fun finish(id: Long, status: String = GameStatus.FINISHED.name, finishedAt: Long, winnerId: Long?)
}