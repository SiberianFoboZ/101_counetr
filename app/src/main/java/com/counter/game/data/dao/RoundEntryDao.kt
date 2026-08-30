package com.counter.game.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.counter.game.data.entity.RoundEntryEntity
import com.counter.game.data.entity.RoundCardEntity
import kotlinx.coroutines.flow.Flow

data class RoundEntryWithCards(
    val entryId: Long,
    val roundId: Long,
    val gamePlayerId: Long,
    val deltaScore: Int,
    val rawInputJson: String?,
    val cardCode: String?,
    val cardCount: Int?,
)

/**
 * Плоский список «раунд → игрок → дельта → (карты)», для экрана истории.
 * Карты размножают строки (LEFT JOIN), но `delta_score` у всех строк одного entry одинаков.
 */
data class RoundEntryCard(
    val entryId: Long,
    val roundId: Long,
    val roundNumber: Int,
    val gamePlayerId: Long,
    val deltaScore: Int,
    val cardCode: String?,
    val cardCount: Int?,
)

@Dao
interface RoundEntryDao {
    @Query(
        """
        SELECT re.id AS entryId, re.round_id AS roundId, re.game_player_id AS gamePlayerId,
               re.delta_score AS deltaScore, re.raw_input_json AS rawInputJson,
               rc.card_code AS cardCode, rc.count AS cardCount
        FROM round_entries re
        INNER JOIN rounds r ON r.id = re.round_id
        LEFT JOIN round_cards rc ON rc.round_entry_id = re.id
        WHERE r.game_id = :gameId
        ORDER BY re.round_id ASC, re.game_player_id ASC
        """,
    )
    fun observeWithCards(gameId: Long): Flow<List<RoundEntryWithCards>>

    /**
     * Все entry для игры с размноженными картами. Используется для экрана истории,
     * где нужен весь набор данных по каждому раунду.
     */
    @Query(
        """
        SELECT re.id AS entryId, re.round_id AS roundId, r.round_number AS roundNumber,
               re.game_player_id AS gamePlayerId, re.delta_score AS deltaScore,
               rc.card_code AS cardCode, rc.count AS cardCount
        FROM round_entries re
        INNER JOIN rounds r ON r.id = re.round_id
        LEFT JOIN round_cards rc ON rc.round_entry_id = re.id
        WHERE r.game_id = :gameId
        ORDER BY r.round_number ASC, re.game_player_id ASC
        """,
    )
    fun observeCards(gameId: Long): Flow<List<RoundEntryCard>>

    @Insert
    suspend fun insertEntry(entry: RoundEntryEntity): Long

    @Insert
    suspend fun insertCards(cards: List<RoundCardEntity>)

    @Query("UPDATE round_entries SET delta_score = :delta WHERE id = :id")
    suspend fun updateDelta(id: Long, delta: Int)
}