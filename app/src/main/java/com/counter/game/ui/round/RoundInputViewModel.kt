package com.counter.game.ui.round

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.entity.GameStatus
import com.counter.game.data.entity.SettingsEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoundInputState(
    val players: List<GamePlayerWithScore> = emptyList(),
    val settings: SettingsEntity = SettingsEntity(),
    val threshold: Int = 101,
    val winnerGamePlayerId: Long? = null,
    val handCounts: Map<Long, Map<String, Int>> = emptyMap(),
    val suitDialog: SuitDialogState? = null,
    val isSaving: Boolean = false,
)

data class SuitDialogState(
    val gamePlayerId: Long,
    val currentCode: String, // "Q_hearts", "Q_diamonds", "Q_clubs", "Q_spades" или K_*
    val targetCode: String,  // что показать в попапе для подтверждения (после выбора масти)
    val suit: String?,        // выбранная масть: spades/hearts/diamonds/clubs/null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoundInputViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)
    private val handFlow = MutableStateFlow<Map<Long, Map<String, Int>>>(emptyMap())
    private val winnerFlow = MutableStateFlow<Long?>(null)
    private val suitFlow = MutableStateFlow<SuitDialogState?>(null)
    private val savingFlow = MutableStateFlow(false)

    val state: StateFlow<RoundInputState> = gameIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(RoundInputState()) else gameStateFlow(id)
        }
        .let { src ->
            combine(src, handFlow, winnerFlow, suitFlow, savingFlow) { s, hands, winner, suit, saving ->
                s.copy(
                    handCounts = hands,
                    winnerGamePlayerId = winner,
                    suitDialog = suit,
                    isSaving = saving,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoundInputState())

    private fun gameStateFlow(gameId: Long) = combine(
        container.database.gamePlayerDao().observeWithScores(gameId),
        container.settingsRepository.observe(),
    ) { players, settings ->
        val alive = players.filter { it.totalScore <= (settings?.thresholdScore ?: 101) }
        RoundInputState(
            players = alive,
            settings = settings ?: SettingsEntity(),
            threshold = settings?.thresholdScore ?: 101,
        )
    }

    fun load(gameId: Long) {
        gameIdFlow.value = gameId
    }

    fun selectWinner(id: Long) {
        // Нельзя выбрать выбывшего.
        winnerFlow.value = id
    }

    fun changeCount(gamePlayerId: Long, code: String, count: Int) {
        val current = handFlow.value.toMutableMap()
        val perPlayer = current[gamePlayerId].orEmpty().toMutableMap()
        if (count <= 0) perPlayer.remove(code) else perPlayer[code] = count
        if (perPlayer.isEmpty()) current.remove(gamePlayerId) else current[gamePlayerId] = perPlayer
        handFlow.value = current

        // Если это единственная карта на руках и это дама/король — попап масти.
        val total = perPlayer.values.sum()
        val only = perPlayer.entries.firstOrNull()
        if (total == 1 && only != null) {
            val key = only.key
            if (key.startsWith("Q_") || key.startsWith("K_")) {
                suitFlow.value = SuitDialogState(
                    gamePlayerId = gamePlayerId,
                    currentCode = key,
                    targetCode = key,
                    suit = null,
                )
            }
        } else {
            // Сбросить попап, если был.
            if (suitFlow.value?.gamePlayerId == gamePlayerId) suitFlow.value = null
        }
    }

    fun chooseSuit(suit: String) {
        val dialog = suitFlow.value ?: return
        val newCode = "${dialog.currentCode.substringBefore('_')}_${suit}"
        val perPlayer = handFlow.value[dialog.gamePlayerId].orEmpty().toMutableMap()
        perPlayer.remove(dialog.currentCode)
        perPlayer[newCode] = 1
        handFlow.value = handFlow.value.toMutableMap().also { it[dialog.gamePlayerId] = perPlayer }
        suitFlow.value = null
    }

    fun dismissSuitDialog() {
        suitFlow.value = null
    }

    fun save() {
        val gameId = gameIdFlow.value ?: return
        if (savingFlow.value) return
        savingFlow.value = true
        viewModelScope.launch {
            try {
                container.gamesRepository.saveRound(
                    com.counter.game.data.repo.RoundInput(
                        gameId = gameId,
                        winnerGamePlayerId = winnerFlow.value,
                        hands = handFlow.value,
                    ),
                )
                savingFlow.value = false
                handFlow.value = emptyMap()
                winnerFlow.value = null
            } catch (t: Throwable) {
                savingFlow.value = false
            }
        }
    }
}