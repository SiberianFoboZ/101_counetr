package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.counter.game.data.entity.GameEntity
import com.counter.game.data.entity.GameStatus
import kotlinx.coroutines.flow.Flow

/**
 * Игра + имя победителя (если есть), для экрана статистики.
 */
data class GameWithWinner(
    val id: Long,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val thresholdScore: Int,
    val winnerPlayerId: Long?,
    val winnerName: String?,
)

/**
 * Агрегат по игрокам: число побед и число игр (включая текущие).
 */
data class PlayerStatsRow(
    val playerId: Long,
    val name: String,
    val wins: Int,
    val games: Int,
)

/**
 * Одна строка в детальной истории игрока: игра + (опционально) раунд с дельтой этого игрока.
 * Несколько строк на одну игру — по числу раундов.
 */
data class PlayerGameHistoryRow(
    val gameId: Long,
    val startedAt: Long,
    val status: String,
    val thresholdScore: Int,
    val winnerPlayerId: Long?,
    val winnerName: String?,
    val roundNumber: Int?,
    val delta: Int?,
)

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

    @Query("SELECT * FROM games ORDER BY started_at DESC")
    fun observeAllDesc(): Flow<List<GameEntity>>

    @Query(
        """
        SELECT g.id AS id, g.status AS status, g.started_at AS startedAt,
               g.finished_at AS finishedAt, g.threshold_score AS thresholdScore,
               g.winner_player_id AS winnerPlayerId, p.name AS winnerName
        FROM games g
        LEFT JOIN players p ON p.id = g.winner_player_id
        ORDER BY g.started_at DESC
        """,
    )
    fun observeAllWithWinner(): Flow<List<GameWithWinner>>

    /**
     * Полная история игрока: список игр, в которых он участвовал, с дельтами по раундам.
     * LEFT JOIN rounds/round_entries даёт строку на каждый раунд; если раундов ещё нет — null.
     */
    @Query(
        """
        SELECT g.id AS gameId, g.started_at AS startedAt, g.status AS status,
               g.threshold_score AS thresholdScore, g.winner_player_id AS winnerPlayerId,
               pw.name AS winnerName,
               r.round_number AS roundNumber, re.delta_score AS delta
        FROM games g
        JOIN game_players gp ON gp.game_id = g.id AND gp.player_id = :playerId
        LEFT JOIN rounds r ON r.game_id = g.id
        LEFT JOIN round_entries re ON re.round_id = r.id AND re.game_player_id = gp.id
        LEFT JOIN players pw ON pw.id = g.winner_player_id
        ORDER BY g.started_at DESC, r.round_number ASC
        """,
    )
    fun observeHistoryForPlayer(playerId: Long): Flow<List<PlayerGameHistoryRow>>

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE games SET status = :status, finished_at = :finishedAt, winner_player_id = :winnerId WHERE id = :id")
    suspend fun finish(id: Long, status: String = GameStatus.FINISHED.name, finishedAt: Long, winnerId: Long?)
}