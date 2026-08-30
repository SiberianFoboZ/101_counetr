package com.counter.game.ui.round

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.data.entity.SettingsEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoundInputState(
    val players: List<GamePlayerWithScore> = emptyList(),
    val cardDefs: Map<String, CardDefinitionEntity> = emptyMap(),
    val settings: SettingsEntity = SettingsEntity(),
    val threshold: Int = 101,
    val winnerGamePlayerId: Long? = null,
    val winnerDelta: Int = 0,
    val handCounts: Map<Long, Map<String, Int>> = emptyMap(),
    val isSaving: Boolean = false,
    val savedRoundNumber: Int? = null,
    val limitMessage: String? = null,
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@OptIn(ExperimentalCoroutinesApi::class)
class RoundInputViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)
    private val handFlow = MutableStateFlow<Map<Long, Map<String, Int>>>(emptyMap())
    private val winnerFlow = MutableStateFlow<Long?>(null)
    private val winnerDeltaFlow = MutableStateFlow(0)
    private val savingFlow = MutableStateFlow(false)
    private val savedRoundFlow = MutableStateFlow<Int?>(null)
    private val _limitMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<RoundInputState> = gameIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(RoundInputState()) else gameStateFlow(id)
        }
        .let { src ->
            combine(
                combine(
                    combine(
                        combine(src, handFlow, winnerFlow) { s, h, w -> Triple(s, h, w) },
                        winnerDeltaFlow,
                    ) { triple, wd -> Quad(triple.first, triple.second, triple.third, wd) },
                    _limitMessage,
                ) { q, msg -> Quint(q.first, q.second, q.third, q.fourth, msg) },
                combine(savingFlow, savedRoundFlow) { s, n -> s to n },
            ) { left, right ->
                val s = left.first
                val hands = left.second
                val winner = left.third
                val winnerDelta = left.fourth
                val limitMsg = left.fifth
                val saving = right.first
                val savedRound = right.second
                s.copy(
                    handCounts = hands,
                    winnerGamePlayerId = winner,
                    winnerDelta = winnerDelta,
                    isSaving = saving,
                    savedRoundNumber = savedRound,
                    limitMessage = limitMsg,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoundInputState())

    private fun gameStateFlow(gameId: Long): kotlinx.coroutines.flow.Flow<RoundInputState> = combine(
        container.database.gamePlayerDao().observeWithScores(gameId),
        container.settingsRepository.observe(),
    ) { players, settings ->
        val alive = players.filter { it.totalScore <= (settings?.thresholdScore ?: 101) }
        val cards = container.cardDefinitionsRepository.listAll()
        RoundInputState(
            players = alive,
            cardDefs = cards.associateBy { it.code },
            settings = settings ?: SettingsEntity(),
            threshold = settings?.thresholdScore ?: 101,
        )
    }

    fun load(gameId: Long) {
        gameIdFlow.value = gameId
    }

    fun selectWinner(id: Long) {
        winnerFlow.value = if (winnerFlow.value == id) null else id
        // При смене победителя сбрасываем бонус на 0, чтобы случайно не применить старый.
        winnerDeltaFlow.value = 0
    }

    /**
     * Бонус победителю за раунд. По умолчанию 0 (без бонуса). Допустимые значения:
     * 0, -20, -40, -50 — отрицательные «снимают» очки с победителя, как бонус за чистую победу.
     */
    fun setWinnerDelta(value: Int) {
        winnerDeltaFlow.value = value
    }

    fun changeCount(gamePlayerId: Long, code: String, count: Int) {
        val current = handFlow.value.toMutableMap()
        val perPlayer = current[gamePlayerId].orEmpty().toMutableMap()
        if (count <= 0) perPlayer.remove(code) else perPlayer[code] = count
        if (perPlayer.isEmpty()) current.remove(gamePlayerId) else current[gamePlayerId] = perPlayer
        handFlow.value = current
    }

    /**
     * Попытка увеличить счётчик карты на 1. Если лимит превышен, в `state.limitMessage`
     * появляется сообщение для snackbar.
     */
    fun tryAdd(gamePlayerId: Long, code: String) {
        val current = handFlow.value.toMutableMap()
        val perPlayer = current[gamePlayerId].orEmpty().toMutableMap()
        // Штрафы победителя действуют только на проигравших. Победителю можно редактировать свободно.
        val effectiveWinnerDelta = if (gamePlayerId == winnerFlow.value) 0 else winnerDeltaFlow.value
        when (val result = canAddCard(current, gamePlayerId, code, effectiveWinnerDelta)) {
            is AddCardResult.Ok -> {
                perPlayer[code] = (perPlayer[code] ?: 0) + 1
                current[gamePlayerId] = perPlayer
                handFlow.value = current
            }
            is AddCardResult.CodeLimitExceeded -> {
                _limitMessage.value = "Уже ${result.current} шт., максимум ${result.max} для этой карты"
            }
            is AddCardResult.NominalLimitExceeded -> {
                _limitMessage.value = "Уже ${result.current} таких карт в раунде, максимум ${result.max}"
            }
            is AddCardResult.BlockedByWinnerDelta -> {
                _limitMessage.value = result.reason
            }
        }
    }

    /**
     * Попытка уменьшить счётчик карты на 1. Если код не найден — no-op.
     */
    fun tryRemove(gamePlayerId: Long, code: String) {
        val current = handFlow.value.toMutableMap()
        val perPlayer = current[gamePlayerId].orEmpty().toMutableMap()
        val v = (perPlayer[code] ?: 0) - 1
        if (v <= 0) perPlayer.remove(code) else perPlayer[code] = v
        if (perPlayer.isEmpty()) current.remove(gamePlayerId) else current[gamePlayerId] = perPlayer
        handFlow.value = current
    }

    fun consumeLimitMessage() {
        _limitMessage.value = null
    }

    fun save() {
        val gameId = gameIdFlow.value ?: return
        if (savingFlow.value) return
        if (handFlow.value.isEmpty() && winnerFlow.value == null) return

        savingFlow.value = true
        viewModelScope.launch {
            try {
                val result = container.gamesRepository.saveRound(
                    com.counter.game.data.repo.RoundInput(
                        gameId = gameId,
                        winnerGamePlayerId = winnerFlow.value,
                        winnerDelta = winnerDeltaFlow.value,
                        hands = handFlow.value,
                    ),
                )
                savedRoundFlow.value = result.roundNumber
                handFlow.value = emptyMap()
                winnerFlow.value = null
                winnerDeltaFlow.value = 0
                savingFlow.value = false
            } catch (t: Throwable) {
                savingFlow.value = false
            }
        }
    }

    fun consumeSaved() {
        savedRoundFlow.value = null
    }
}