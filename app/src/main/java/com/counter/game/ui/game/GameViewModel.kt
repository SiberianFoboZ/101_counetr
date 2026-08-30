package com.counter.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.data.entity.GameEntity
import com.counter.game.data.entity.GameStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameState(
    val game: GameEntity? = null,
    val players: List<GamePlayerWithScore> = emptyList(),
    val winnerName: String? = null,
    val threshold: Int = 101,
)

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(private val container: AppContainer) : ViewModel() {

    private val gameIdFlow = MutableStateFlow<Long?>(null)

    val state: StateFlow<GameState> = gameIdFlow
        .flatMapLatest { id ->
            if (id == null) flowOf(GameState()) else gameStateFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameState())

    private fun gameStateFlow(gameId: Long): Flow<GameState> = combine(
        container.database.gameDao().observe(gameId),
        container.database.gamePlayerDao().observeWithScores(gameId),
        container.settingsRepository.observe(),
    ) { game, players, settings ->
        val winnerName = if (game?.status == GameStatus.FINISHED.name && game.winnerPlayerId != null) {
            container.database.playerDao().byId(game.winnerPlayerId)?.name
        } else null
        GameState(
            game = game,
            players = players,
            winnerName = winnerName,
            threshold = game?.thresholdScore ?: (settings?.thresholdScore ?: 101),
        )
    }

    fun load(gameId: Long) {
        gameIdFlow.value = gameId
    }

    fun pauseAndExit(after: () -> Unit) {
        val id = gameIdFlow.value ?: return
        viewModelScope.launch {
            container.gamesRepository.pause(id)
            after()
        }
    }
}