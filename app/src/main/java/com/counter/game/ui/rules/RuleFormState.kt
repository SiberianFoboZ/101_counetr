package com.counter.game.ui.rules

import com.counter.game.engine.RuleSerializer

/**
 * Состояние формы редактирования правила. Полностью отвязано от Room —
 * конвертируется в [RuleSerializer.Form] перед сохранением и обратно при загрузке.
 */
data class RuleFormState(
    val ruleId: Long? = null,
    val name: String = "",
    val appliesToCard: String = "",
    val form: RuleSerializer.Form = RuleSerializer.Form(),
) {
    val isExisting: Boolean get() = ruleId != null
    val canSave: Boolean get() = when (form.kind) {
        // Финальная корректировка: имя обязательно, карта не нужна.
        RuleSerializer.Kind.FINAL_ADJUSTMENT -> name.isNotBlank()
        RuleSerializer.Kind.PER_CARD -> name.isNotBlank() && appliesToCard.isNotEmpty()
    }
}