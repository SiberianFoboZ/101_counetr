package com.counter.game.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.PlayerGameHistoryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Одна игра в детальной истории игрока.
 */
data class PlayerGameHistory(
    val gameId: Long,
    val startedAt: Long,
    val status: String,
    val thresholdScore: Int,
    val winnerName: String?,
    val isWin: Boolean,
    val rounds: List<RoundDelta>,
    val totalDelta: Int,
)

data class RoundDelta(
    val roundNumber: Int,
    val delta: Int,
)

data class PlayerDetailState(
    val playerId: Long = 0,
    val playerName: String = "",
    val wins: Int = 0,
    val games: Int = 0,
    val history: List<PlayerGameHistory> = emptyList(),
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayerDetailViewModel(
    private val container: AppContainer,
    private val playerId: Long,
) : ViewModel() {

    private val playerIdFlow = MutableStateFlow(playerId)

    val state: StateFlow<PlayerDetailState> = playerIdFlow
        .flatMapLatest { id -> if (id <= 0L) flowOf(PlayerDetailState()) else buildState(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerDetailState())

    private fun buildState(id: Long) = combine(
        container.database.playerDao().observeStats(),
        container.database.gameDao().observeHistoryForPlayer(id),
    ) { stats, rows ->
        val myStats = stats.firstOrNull { it.playerId == id }
        PlayerDetailState(
            playerId = id,
            playerName = myStats?.name ?: "",
            wins = myStats?.wins ?: 0,
            games = myStats?.games ?: 0,
            history = groupHistory(rows),
        )
    }

    private fun groupHistory(rows: List<PlayerGameHistoryRow>): List<PlayerGameHistory> {
        if (rows.isEmpty()) return emptyList()
        val byGame: MutableMap<Long, MutableList<PlayerGameHistoryRow>> = linkedMapOf()
        val order: MutableList<Long> = mutableListOf()
        val startedAt: MutableMap<Long, Long> = mutableMapOf()
        val status: MutableMap<Long, String> = mutableMapOf()
        val threshold: MutableMap<Long, Int> = mutableMapOf()
        val winnerName: MutableMap<Long, String?> = mutableMapOf()
        val winnerId: MutableMap<Long, Long?> = mutableMapOf()
        for (r in rows) {
            if (r.gameId !in byGame) {
                order.add(r.gameId)
                startedAt[r.gameId] = r.startedAt
                status[r.gameId] = r.status
                threshold[r.gameId] = r.thresholdScore
                winnerName[r.gameId] = r.winnerName
                winnerId[r.gameId] = r.winnerPlayerId
            }
            byGame.getOrPut(r.gameId) { mutableListOf() }.add(r)
        }
        return order.map { gid ->
            val items = byGame[gid].orEmpty()
            val rounds = items.mapNotNull { row ->
                val rn = row.roundNumber ?: return@mapNotNull null
                val delta = row.delta ?: return@mapNotNull null
                RoundDelta(roundNumber = rn, delta = delta)
            }.sortedBy { it.roundNumber }
            val total = rounds.sumOf { it.delta }
            val wId = winnerId[gid]
            PlayerGameHistory(
                gameId = gid,
                startedAt = startedAt[gid] ?: 0L,
                status = status[gid] ?: "IN_PROGRESS",
                thresholdScore = threshold[gid] ?: 101,
                winnerName = winnerName[gid],
                isWin = wId != null && rounds.isNotEmpty(),
                rounds = rounds,
                totalDelta = total,
            )
        }
    }
}