package com.counter.game.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.entity.RuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RulesState(
    val rules: List<RuleEntity> = emptyList(),
    val cardLabels: Map<String, String> = emptyMap(),
    val pendingDelete: RuleEntity? = null,
)

class RulesViewModel(private val container: AppContainer) : ViewModel() {

    private val _local = MutableStateFlow<RulesState>(RulesState())

    val state: StateFlow<RulesState> = combine(
        container.rulesRepository.observeAll(),
        _local,
    ) { rules, local ->
        val knownLabels = rules.associate { it.appliesToCard to it.appliesToCard }
        local.copy(rules = rules, cardLabels = local.cardLabels + knownLabels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesState())

    init {
        viewModelScope.launch {
            val cards = container.cardDefinitionsRepository.listAll().associate { it.code to it.label }
            _local.value = _local.value.copy(cardLabels = _local.value.cardLabels + cards)
        }
    }

    fun toggleEnabled(rule: RuleEntity) {
        viewModelScope.launch {
            container.rulesRepository.setEnabled(rule.id, !rule.enabled)
        }
    }

    fun movePriority(rule: RuleEntity, delta: Int) {
        viewModelScope.launch {
            val target = (rule.priority + delta).coerceAtLeast(MIN_PRIORITY)
            container.rulesRepository.update(rule.copy(priority = target))
        }
    }

    fun requestDelete(rule: RuleEntity) {
        _local.value = _local.value.copy(pendingDelete = rule)
    }

    fun cancelDelete() {
        _local.value = _local.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        val r = _local.value.pendingDelete ?: return
        viewModelScope.launch {
            container.rulesRepository.delete(r)
            _local.value = _local.value.copy(pendingDelete = null)
        }
    }

    companion object {
        const val MIN_PRIORITY = -1000
    }
}