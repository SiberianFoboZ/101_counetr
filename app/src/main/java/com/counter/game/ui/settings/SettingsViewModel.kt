package com.counter.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.entity.SettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUi(
    val queenValue: Int = 3,
    val queenSpadesValue: Int = 25,
    val kingValue: Int = 4,
    val thresholdScore: Int = 101,
    val fontScale: Float = 1.0f,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUi())
    val state: StateFlow<SettingsUi> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = container.settingsRepository.get()
            _state.value = s.toUi()
        }
    }

    fun updateQueen(value: Int) { _state.value = _state.value.copy(queenValue = value) }
    fun updateQueenSpades(value: Int) { _state.value = _state.value.copy(queenSpadesValue = value) }
    fun updateKing(value: Int) { _state.value = _state.value.copy(kingValue = value) }
    fun updateThreshold(value: Int) { _state.value = _state.value.copy(thresholdScore = value) }
    fun updateFontScale(value: Float) { _state.value = _state.value.copy(fontScale = value) }

    fun save() {
        viewModelScope.launch {
            val ui = _state.value
            container.settingsRepository.update(
                queenValue = ui.queenValue,
                queenSpadesValue = ui.queenSpadesValue,
                kingValue = ui.kingValue,
                thresholdScore = ui.thresholdScore,
                fontScale = ui.fontScale,
            )
        }
    }

    private fun SettingsEntity.toUi() = SettingsUi(
        queenValue = queenValue,
        queenSpadesValue = queenSpadesValue,
        kingValue = kingValue,
        thresholdScore = thresholdScore,
        fontScale = fontScale,
    )
}