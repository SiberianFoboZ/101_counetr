package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.counter.game.data.entity.GamePlayerEntity
import kotlinx.coroutines.flow.Flow

data class GamePlayerWithScore(
    val id: Long,
    val playerId: Long,
    val displayName: String,
    val seatOrder: Int,
    val totalScore: Int,
)

@Dao
interface GamePlayerDao {
    @Query("SELECT * FROM game_players WHERE game_id = :gameId ORDER BY seat_order ASC")
    fun observeByGame(gameId: Long): Flow<List<GamePlayerEntity>>

    @Query("SELECT * FROM game_players WHERE game_id = :gameId ORDER BY seat_order ASC")
    suspend fun listByGame(gameId: Long): List<GamePlayerEntity>

    @Query("SELECT * FROM game_players WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): GamePlayerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(players: List<GamePlayerEntity>): List<Long>

    @Query(
        """
        SELECT gp.id AS id, gp.player_id AS playerId, gp.display_name AS displayName,
               gp.seat_order AS seatOrder,
               COALESCE((
                   SELECT SUM(re.delta_score) FROM round_entries re
                   JOIN rounds r ON r.id = re.round_id
                   WHERE re.game_player_id = gp.id AND r.game_id = gp.game_id
               ), 0) AS totalScore
        FROM game_players gp
        WHERE gp.game_id = :gameId
        ORDER BY gp.seat_order ASC
        """,
    )
    fun observeWithScores(gameId: Long): Flow<List<GamePlayerWithScore>>

    @Query(
        """
        SELECT COALESCE((
            SELECT SUM(re.delta_score) FROM round_entries re
            JOIN rounds r ON r.id = re.round_id
            WHERE re.game_player_id = :gamePlayerId AND r.game_id = :gameId
        ), 0)
        """,
    )
    suspend fun totalScore(gameId: Long, gamePlayerId: Long): Int
}