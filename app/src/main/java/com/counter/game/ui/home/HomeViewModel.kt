package com.counter.game.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.repo.GamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            val id = container.gamesRepository.latestPausedId()
            if (id != null) container.gamesRepository.resume(id)
            onResult(id)
        }
    }
}