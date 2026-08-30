package com.counter.game.ui.newgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.entity.PlayerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewGameState(
    val players: List<PlayerEntity> = emptyMap<Long, PlayerEntity>().let { emptyList() },
    val selectedIds: Set<Long> = emptySet(),
    val isStarting: Boolean = false,
    val startedGameId: Long? = null,
    val errorMessage: String? = null,
)

class NewGameViewModel(private val container: AppContainer) : ViewModel() {

    private val selectedFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val transientFlow = MutableStateFlow(Transient())

    val state: StateFlow<NewGameState> = combine(
        container.playersRepository.observeActive(),
        selectedFlow,
        transientFlow,
    ) { players, selected, transient ->
        NewGameState(
            players = players,
            selectedIds = selected,
            isStarting = transient.isStarting,
            startedGameId = transient.startedGameId,
            errorMessage = transient.errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewGameState())

    fun toggle(playerId: Long) {
        val current = selectedFlow.value
        selectedFlow.value = if (playerId in current) current - playerId else current + playerId
    }

    fun addInline(name: String = ""): Long {
        // Создаём сразу, чтобы UI мог его увидеть и показать как «нового».
        var id = -1L
        viewModelScope.launch {
            id = container.playersRepository.add(name)
            selectedFlow.value = selectedFlow.value + id
        }
        return id
    }

    fun start() {
        val ids = selectedFlow.value.toList()
        if (ids.size !in 2..10) {
            transientFlow.value = transientFlow.value.copy(
                errorMessage = "Выберите от 2 до 10 игроков (сейчас ${ids.size})",
            )
            return
        }
        transientFlow.value = transientFlow.value.copy(isStarting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val settings = container.settingsRepository.get()
                val names = container.playersRepository.listActive()
                    .filter { it.id in ids }
                    .associate { it.id to it.name }
                val gameId = container.gamesRepository.startGame(
                    com.counter.game.data.repo.NewGameInput(
                        thresholdScore = settings.thresholdScore,
                        selectedPlayerIds = ids,
                        displayNames = names,
                    ),
                )
                transientFlow.value = transientFlow.value.copy(isStarting = false, startedGameId = gameId)
            } catch (t: Throwable) {
                transientFlow.value = transientFlow.value.copy(isStarting = false, errorMessage = t.message)
            }
        }
    }

    fun consumeStartedGame() {
        transientFlow.value = transientFlow.value.copy(startedGameId = null)
    }

    fun consumeError() {
        transientFlow.value = transientFlow.value.copy(errorMessage = null)
    }

    private data class Transient(
        val isStarting: Boolean = false,
        val startedGameId: Long? = null,
        val errorMessage: String? = null,
    )
}