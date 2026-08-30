package com.counter.game.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.counter.game.AppContainer
import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.data.entity.RuleEntity
import com.counter.game.engine.RuleSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RuleEditState(
    val form: RuleFormState = RuleFormState(),
    val cards: List<CardDefinitionEntity> = emptyList(),
    val saved: Boolean = false,
    val confirmDelete: Boolean = false,
)

class RuleEditViewModel(
    private val container: AppContainer,
    private val ruleId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(RuleEditState())
    val state: StateFlow<RuleEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cards = container.cardDefinitionsRepository.listAll()
            val initial = if (ruleId == null || ruleId < 0) {
                RuleFormState()
            } else {
                val rule = container.rulesRepository.getById(ruleId)
                if (rule == null) RuleFormState() else hydrate(rule)
            }
            _state.value = _state.value.copy(form = initial, cards = cards)
        }
    }

    private fun hydrate(rule: RuleEntity): RuleFormState {
        val form = RuleSerializer.decode(rule.definitionJson)
            ?: RuleSerializer.Form()
        return RuleFormState(
            ruleId = rule.id,
            name = rule.name,
            appliesToCard = rule.appliesToCard,
            form = form,
        )
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(form = _state.value.form.copy(name = value))
    }

    fun setAppliesToCard(code: String) {
        _state.value = _state.value.copy(form = _state.value.form.copy(appliesToCard = code))
    }

    fun setNominal(form: RuleSerializer.NominalForm) {
        _state.value = _state.value.copy(form = _state.value.form.copy(form = _state.value.form.form.copy(nominal = form)))
    }

    fun setSuit(form: RuleSerializer.SuitForm) {
        _state.value = _state.value.copy(form = _state.value.form.copy(form = _state.value.form.form.copy(suit = form)))
    }

    fun setConditionEnabled(enabled: Boolean) {
        val current = _state.value.form
        val newForm = if (enabled) {
            current.form.copy(condition = current.form.condition ?: RuleSerializer.ConditionForm())
        } else {
            current.form.copy(condition = null)
        }
        _state.value = _state.value.copy(form = current.copy(form = newForm))
    }

    fun setConditionOp(op: RuleSerializer.OpForm) {
        val cond = _state.value.form.form.condition ?: RuleSerializer.ConditionForm()
        _state.value = _state.value.copy(
            form = _state.value.form.copy(
                form = _state.value.form.form.copy(condition = cond.copy(op = op)),
            ),
        )
    }

    fun setConditionLeft(left: RuleSerializer.OperandForm) {
        val cond = _state.value.form.form.condition ?: RuleSerializer.ConditionForm()
        _state.value = _state.value.copy(
            form = _state.value.form.copy(
                form = _state.value.form.form.copy(condition = cond.copy(left = left)),
            ),
        )
    }

    fun setConditionRight(value: Int) {
        val cond = _state.value.form.form.condition ?: RuleSerializer.ConditionForm()
        _state.value = _state.value.copy(
            form = _state.value.form.copy(
                form = _state.value.form.form.copy(condition = cond.copy(rightValue = value)),
            ),
        )
    }

    fun setThenAction(action: RuleSerializer.ActionForm) {
        _state.value = _state.value.copy(form = _state.value.form.copy(form = _state.value.form.form.copy(then = action)))
    }

    fun setElseAction(action: RuleSerializer.ActionForm) {
        _state.value = _state.value.copy(
            form = _state.value.form.copy(form = _state.value.form.form.copy(elseAction = action)),
        )
    }

    fun setKind(kind: RuleSerializer.Kind) {
        // При смене kind сбрасываем action к дефолту для нового типа.
        val current = _state.value.form.form
        val newForm = when (kind) {
            RuleSerializer.Kind.PER_CARD -> current.copy(
                then = if (current.then is RuleSerializer.ActionForm.SubtractTotal) RuleSerializer.ActionForm.BaseValue else current.then,
                elseAction = if (current.elseAction is RuleSerializer.ActionForm.SubtractTotal) RuleSerializer.ActionForm.BaseValue else current.elseAction,
                kind = kind,
            )
            RuleSerializer.Kind.FINAL_ADJUSTMENT -> current.copy(
                then = RuleSerializer.ActionForm.SubtractTotal,
                elseAction = RuleSerializer.ActionForm.SubtractTotal,
                kind = kind,
            )
        }
        _state.value = _state.value.copy(form = _state.value.form.copy(form = newForm))
    }

    fun save() {
        val current = _state.value.form
        if (!current.canSave) return
        viewModelScope.launch {
            val priority = if (current.isExisting) {
                val existing = container.rulesRepository.getById(current.ruleId!!)
                existing?.priority ?: 0
            } else {
                val all = container.rulesRepository.observeAll().first()
                (all.minOfOrNull { it.priority } ?: 0) - 1
            }
            val entity = RuleEntity(
                id = current.ruleId ?: 0,
                name = current.name.trim(),
                // Для FINAL_ADJUSTMENT карта не используется — ставим заглушку, чтобы DB не ругалась.
                appliesToCard = current.appliesToCard.ifEmpty {
                    if (current.form.kind == RuleSerializer.Kind.FINAL_ADJUSTMENT) "_final" else ""
                },
                priority = priority,
                enabled = true,
                definitionJson = RuleSerializer.encode(current.form),
            )
            container.rulesRepository.upsert(entity)
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun requestDelete() {
        if (_state.value.form.isExisting) {
            _state.value = _state.value.copy(confirmDelete = true)
        }
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(confirmDelete = false)
    }

    fun confirmDelete() {
        val id = _state.value.form.ruleId ?: return
        viewModelScope.launch {
            val rule = container.rulesRepository.getById(id) ?: return@launch
            container.rulesRepository.delete(rule)
            _state.value = _state.value.copy(confirmDelete = false, saved = true)
        }
    }
}