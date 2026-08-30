package com.counter.game.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.dao.RoundEntryWithCards
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow

data class HistoryState(
    val players: List<GamePlayerWithScore> = emptyList(),
    val rounds: List<List<Int?>> = emptyList(), // rounds.size = N rounds; rounds[r][playerIndex] = delta or null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryState> = gameIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(HistoryState()) else buildState(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    private fun buildState(gameId: Long): Flow<HistoryState> = combine(
        container.database.gamePlayerDao().observeWithScores(gameId),
        container.database.roundEntryDao().observeWithCards(gameId),
    ) { players, entries ->
        HistoryState(players = players, rounds = groupByRound(players, entries))
    }

    private fun groupByRound(
        players: List<GamePlayerWithScore>,
        entries: List<RoundEntryWithCards>,
    ): List<List<Int?>> {
        // roundId → gamePlayerId → deltaScore (агрегат).
        val byRound: MutableMap<Long, MutableMap<Long, Int>> = linkedMapOf()
        val byRoundOrder: MutableMap<Long, Long> = linkedMapOf() // roundId → первый встретившийся round_id (для сохранения порядка)
        // entries приходят упорядоченные по round_id ASC; сохраним порядок встречи раундов.
        var roundOrder = 0L
        var lastRoundId: Long = -1
        for (e in entries) {
            if (e.roundId != lastRoundId) {
                roundOrder += 1
                byRoundOrder[e.roundId] = roundOrder
                byRound[e.roundId] = linkedMapOf()
                lastRoundId = e.roundId
            }
            val inner = byRound[e.roundId]!!
            inner[e.gamePlayerId] = e.deltaScore
        }
        // Если у игрока в этом раунде нет записи — null.
        val orderedRoundIds = byRound.keys.toList()
        return orderedRoundIds.map { rid ->
            val inner = byRound[rid]!!
            players.map { p -> inner[p.id] }
        }
    }

    fun load(gameId: Long) {
        gameIdFlow.value = gameId
    }
}