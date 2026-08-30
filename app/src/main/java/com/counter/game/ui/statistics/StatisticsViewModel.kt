package com.counter.game.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.dao.GameWithWinner
import com.counter.game.data.dao.PlayerStatsRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatisticsState(
    val games: List<GameWithWinner> = emptyList(),
    val players: List<PlayerStatsRow> = emptyList(),
)

class StatisticsViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<StatisticsState> = combine(
        container.database.gameDao().observeAllWithWinner(),
        container.database.playerDao().observeStats(),
    ) { games, players ->
        StatisticsState(games = games, players = players)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsState())
}