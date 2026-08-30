package com.counter.game.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.entity.PlayerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayersState(
    val items: List<PlayerEntity> = emptyList(),
    val renaming: PlayerEntity? = null,
    val archiving: PlayerEntity? = null,
)

class PlayersViewModel(private val container: AppContainer) : ViewModel() {

    private val renamingFlow = MutableStateFlow<PlayerEntity?>(null)
    private val archivingFlow = MutableStateFlow<PlayerEntity?>(null)

    val state: StateFlow<PlayersState> = combine(
        container.playersRepository.observeActive(),
        renamingFlow,
        archivingFlow,
    ) { items, renaming, archiving ->
        PlayersState(items = items, renaming = renaming, archiving = archiving)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayersState())

    fun add() {
        viewModelScope.launch {
            container.playersRepository.add("")
        }
    }

    fun startRename(player: PlayerEntity) {
        renamingFlow.value = player
    }

    fun cancelRename() {
        renamingFlow.value = null
    }

    fun confirmRename(newName: String) {
        val current = renamingFlow.value ?: return
        viewModelScope.launch {
            container.playersRepository.rename(current.id, newName)
            renamingFlow.value = null
        }
    }

    fun startArchive(player: PlayerEntity) {
        archivingFlow.value = player
    }

    fun cancelArchive() {
        archivingFlow.value = null
    }

    fun confirmArchive() {
        val current = archivingFlow.value ?: return
        viewModelScope.launch {
            container.playersRepository.archive(current.id)
            archivingFlow.value = null
        }
    }
}