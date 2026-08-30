package com.counter.game.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.dao.RoundEntryCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Один раунд для экрана истории.
 */
data class RoundSummary(
    val roundId: Long,
    val roundNumber: Int,
    val winnerName: String?, // null, если ни у кого не 0
    val entries: List<RoundEntry>,
    val totalDelta: Int, // сумма дельт раунда (= 0 всегда, но пригодится)
)

/**
 * Строка раунда: один игрок.
 */
data class RoundEntry(
    val gamePlayerId: Long,
    val playerName: String,
    val delta: Int,
    val isWinner: Boolean, // delta == 0 && != единственный игрок
    val cards: Map<String, Int>,
)

data class HistoryState(
    val players: List<GamePlayerWithScore> = emptyList(),
    val rounds: List<RoundSummary> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryState> = gameIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(HistoryState()) else buildState(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    private fun buildState(gameId: Long) = combine(
        container.database.gamePlayerDao().observeWithScores(gameId),
        container.database.roundEntryDao().observeCards(gameId),
    ) { players, entries ->
        HistoryState(
            players = players,
            rounds = groupRounds(players, entries),
        )
    }

    private fun groupRounds(
        players: List<GamePlayerWithScore>,
        raw: List<RoundEntryCard>,
    ): List<RoundSummary> {
        if (raw.isEmpty()) return emptyList()

        // Шаг 1: дедуплицируем по entryId. LEFT JOIN round_cards размножает строки —
        // для одного entry с N картами в `raw` приходит N строк с одним entryId и разными
        // (cardCode, cardCount). Берём первую строку каждого entryId как «метаданные»
        // (delta + gamePlayerId), карты собираем отдельно.
        val firstByEntry: MutableMap<Long, RoundEntryCard> = linkedMapOf()
        val cardsByEntry: MutableMap<Long, MutableMap<String, Int>> = linkedMapOf()
        val roundIdToEntryIds: MutableMap<Long, MutableSet<Long>> = linkedMapOf()
        val roundIdToNumber: MutableMap<Long, Int> = linkedMapOf()

        for (r in raw) {
            firstByEntry.getOrPut(r.entryId) { r }
            if (r.cardCode != null && r.cardCount != null && r.cardCount > 0) {
                cardsByEntry.getOrPut(r.entryId) { linkedMapOf() }[r.cardCode] = r.cardCount
            }
            roundIdToEntryIds.getOrPut(r.roundId) { linkedSetOf() }.add(r.entryId)
            roundIdToNumber[r.roundId] = r.roundNumber
        }

        // Шаг 2: построить RoundSummary для каждого roundId в порядке round_number.
        val orderedRoundIds = roundIdToNumber.entries
            .sortedBy { it.value }
            .map { it.key }

        val playerById = players.associateBy { it.id }

        return orderedRoundIds.map { rid ->
            val entryIds = roundIdToEntryIds[rid].orEmpty()
            val entries = entryIds.mapNotNull { eid ->
                val meta = firstByEntry[eid] ?: return@mapNotNull null
                val playerName = playerById[meta.gamePlayerId]?.displayName ?: "Игрок"
                RoundEntry(
                    gamePlayerId = meta.gamePlayerId,
                    playerName = playerName,
                    delta = meta.deltaScore,
                    isWinner = false, // заполним ниже
                    cards = cardsByEntry[eid].orEmpty(),
                )
            }
            // Победитель = тот, у кого delta == 0. Если таких нет, победителя нет.
            val winners = entries.filter { it.delta == 0 }
            val winnerName = winners.firstOrNull()?.playerName
            val annotated = entries.map { e ->
                e.copy(isWinner = winners.size == 1 && e.delta == 0)
            }
            RoundSummary(
                roundId = rid,
                roundNumber = roundIdToNumber[rid] ?: 0,
                winnerName = winnerName,
                entries = annotated,
                totalDelta = entries.sumOf { it.delta },
            )
        }
    }

    fun load(gameId: Long) {
        gameIdFlow.value = gameId
    }
}