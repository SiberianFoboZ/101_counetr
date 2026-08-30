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

@Dao
interface RoundEntryDao {
    @Query(
        """
        SELECT re.id AS entryId, re.round_id AS roundId, re.game_player_id AS gamePlayerId,
               re.delta_score AS deltaScore, re.raw_input_json AS rawInputJson,
               rc.card_code AS cardCode, rc.count AS cardCount
        FROM round_entries re
        LEFT JOIN round_cards rc ON rc.round_entry_id = re.id
        WHERE re.round_id IN (SELECT id FROM rounds WHERE game_id = :gameId)
        ORDER BY re.round_id ASC, re.game_player_id ASC
        """,
    )
    fun observeWithCards(gameId: Long): Flow<List<RoundEntryWithCards>>

    @Insert
    suspend fun insertEntry(entry: RoundEntryEntity): Long

    @Insert
    suspend fun insertCards(cards: List<RoundCardEntity>)

    @Query("UPDATE round_entries SET delta_score = :delta WHERE id = :id")
    suspend fun updateDelta(id: Long, delta: Int)
}