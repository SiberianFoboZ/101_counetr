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

/**
 * Режим ввода штрафных очков в раунде.
 * - [CARDS] — обычный режим: по чипу на каждую карту, движок правил считает дельту.
 * - [MANUAL] — быстрый режим: пользователь вводит итоговое число очков вручную на каждого
 *   проигравшего, движок правил пропускается.
 */
enum class InputMode { CARDS, MANUAL }

data class RoundInputState(
    val players: List<GamePlayerWithScore> = emptyList(),
    val cardDefs: Map<String, CardDefinitionEntity> = emptyMap(),
    val settings: SettingsEntity = SettingsEntity(),
    val threshold: Int = 101,
    val inputMode: InputMode = InputMode.CARDS,
    val winnerGamePlayerId: Long? = null,
    val winnerDelta: Int = 0,
    val handCounts: Map<Long, Map<String, Int>> = emptyMap(),
    val manualDelta: Map<Long, Int> = emptyMap(),
    val isSaving: Boolean = false,
    val savedRoundNumber: Int? = null,
    val limitMessage: String? = null,
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
private data class Sext<A, B, C, D, E, F>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoundInputViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)
    private val modeFlow = MutableStateFlow(InputMode.CARDS)
    private val handFlow = MutableStateFlow<Map<Long, Map<String, Int>>>(emptyMap())
    private val manualDeltaFlow = MutableStateFlow<Map<Long, Int>>(emptyMap())
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
                        combine(
                            combine(src, handFlow, winnerFlow) { s, h, w -> Triple(s, h, w) },
                            winnerDeltaFlow,
                        ) { triple, wd -> Quad(triple.first, triple.second, triple.third, wd) },
                        _limitMessage,
                    ) { q, msg -> Quint(q.first, q.second, q.third, q.fourth, msg) },
                    modeFlow,
                ) { q, m -> Sext(q.first, q.second, q.third, q.fourth, q.fifth, m) },
                combine(
                    combine(savingFlow, savedRoundFlow) { s, n -> s to n },
                    manualDeltaFlow,
                ) { sn, md -> Triple(sn.first, sn.second, md) },
            ) { left, right ->
                val s = left.first
                val hands = left.second
                val winner = left.third
                val winnerDelta = left.fourth
                val limitMsg = left.fifth
                val mode = left.sixth
                val saving = right.first
                val savedRound = right.second
                val manual = right.third
                s.copy(
                    inputMode = mode,
                    handCounts = hands,
                    winnerGamePlayerId = winner,
                    winnerDelta = winnerDelta,
                    isSaving = saving,
                    savedRoundNumber = savedRound,
                    limitMessage = limitMsg,
                    manualDelta = manual,
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

    /**
     * Переключение режима ввода. Соглашение — переключение сбрасывает уже введённые данные
     * (карты или ручные числа), чтобы не смешивать две независимые «корзины» в одном раунде.
     * Победитель и его бонус не затрагиваются.
     */
    fun setInputMode(mode: InputMode) {
        if (modeFlow.value == mode) return
        modeFlow.value = mode
        if (mode == InputMode.CARDS) {
            manualDeltaFlow.value = emptyMap()
        } else {
            handFlow.value = emptyMap()
        }
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

    /**
     * Задать дельту вручную для проигравшего (по gamePlayerId).
     * 0 — допустимое значение, удаляем ключ чтобы свёртка оставалась компактной.
     */
    fun setManualDelta(gamePlayerId: Long, value: Int) {
        val current = manualDeltaFlow.value.toMutableMap()
        if (value == 0) current.remove(gamePlayerId) else current[gamePlayerId] = value
        manualDeltaFlow.value = current
    }

    /**
     * Инкремент/декремент ручной дельты (нажатия на «+/−» рядом с числом).
     * Шаг — 1. Значение может быть отрицательным.
     */
    fun changeManualDelta(gamePlayerId: Long, delta: Int) {
        val current = manualDeltaFlow.value.toMutableMap()
        val next = (current[gamePlayerId] ?: 0) + delta
        if (next == 0) current.remove(gamePlayerId) else current[gamePlayerId] = next
        manualDeltaFlow.value = current
    }

    fun consumeLimitMessage() {
        _limitMessage.value = null
    }

    fun save() {
        val gameId = gameIdFlow.value ?: return
        if (savingFlow.value) return
        val mode = modeFlow.value
        if (mode == InputMode.CARDS) {
            if (handFlow.value.isEmpty() && winnerFlow.value == null) return
        } else {
            // В ручном режиме пустые дельты считаем отсутствием ввода — сохранять нечего.
            if (manualDeltaFlow.value.isEmpty() && winnerFlow.value == null) return
        }

        savingFlow.value = true
        viewModelScope.launch {
            try {
                val manual = if (mode == InputMode.MANUAL) manualDeltaFlow.value else null
                val hands = if (mode == InputMode.MANUAL) emptyMap() else handFlow.value
                val result = container.gamesRepository.saveRound(
                    com.counter.game.data.repo.RoundInput(
                        gameId = gameId,
                        winnerGamePlayerId = winnerFlow.value,
                        winnerDelta = winnerDeltaFlow.value,
                        hands = hands,
                        manualDelta = manual,
                    ),
                )
                savedRoundFlow.value = result.roundNumber
                handFlow.value = emptyMap()
                manualDeltaFlow.value = emptyMap()
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
