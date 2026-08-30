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

    /**
     * Число побед и число игр для каждого не-архивного игрока.
     * «Победа» = finished игра с winner_player_id = p.id.
     * «Игры» = число game_players с player_id = p.id.
     */
    @Query(
        """
        SELECT p.id AS playerId, p.name AS name,
               (SELECT COUNT(*) FROM games g
                  WHERE g.winner_player_id = p.id AND g.status = 'FINISHED') AS wins,
               (SELECT COUNT(*) FROM game_players gp WHERE gp.player_id = p.id) AS games
        FROM players p
        WHERE p.is_archived = 0
        ORDER BY wins DESC, games DESC, p.name ASC
        """,
    )
    fun observeStats(): Flow<List<PlayerStatsRow>>
}