package com.counter.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.repo.GamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class HomeState(
    val hasPausedGame: Boolean = false,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val paused = container.gamesRepository.latestPausedId() != null
            _state.value = _state.value.copy(hasPausedGame = paused)
        }
    }

    fun requestResume(onResult: (Long?) -> Unit) {
        // Синхронно: корутина на viewModelScope может быть отменена при popUpTo(Home),
        // и тогда игра останется в PAUSED, а юзер зайдёт в неё снова как в paused.
        val id = runBlocking { container.gamesRepository.latestPausedId() }
        if (id != null) {
            runBlocking { container.gamesRepository.resume(id) }
        }
        onResult(id)
    }
}