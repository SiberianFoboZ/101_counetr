package com.counter.game.ui.rules

import com.counter.game.engine.RuleSerializer

/**
 * Человекочитаемые названия и пояснения для настроек правила.
 * Используется в редакторе, чтобы пользователь понимал, что выбирает.
 */
object RuleLabels {

    data class Choice(val label: String, val description: String)

    val operands: Map<RuleSerializer.OperandForm, Choice> = mapOf(
        RuleSerializer.OperandForm.CARD_COUNT to Choice(
            label = "Сколько этой карты на руках",
            description = "Суммарное число карт этого кода в руке игрока (например, 3 шестёрки).",
        ),
        RuleSerializer.OperandForm.ROUND_DELTA_SO_FAR to Choice(
            label = "Текущий штраф в раунде",
            description = "Сколько очков уже начислили игроку по другим правилам в этом раунде.",
        ),
        RuleSerializer.OperandForm.DISTINCT_CARD_CODES to Choice(
            label = "Разных видов карт",
            description = "Сколько разных кодов карт есть в руке (например, шестёрки + семёрки = 2).",
        ),
        RuleSerializer.OperandForm.TOTAL_SCORE to Choice(
            label = "Итоговый счёт игрока",
            description = "Сколько очков у игрока к этому моменту (до начисления текущего раунда + per-card правила).",
        ),
    )

    val ops: Map<RuleSerializer.OpForm, Choice> = mapOf(
        RuleSerializer.OpForm.EQ to Choice(label = "равно", description = ""),
        RuleSerializer.OpForm.NE to Choice(label = "не равно", description = ""),
        RuleSerializer.OpForm.LT to Choice(label = "меньше", description = ""),
        RuleSerializer.OpForm.LE to Choice(label = "меньше или равно", description = ""),
        RuleSerializer.OpForm.GT to Choice(label = "больше", description = ""),
        RuleSerializer.OpForm.GE to Choice(label = "больше или равно", description = ""),
    )

    val settings: Map<String, Choice> = mapOf(
        "queen_value" to Choice(label = "queen_value", description = "Номинал дамы (не пик) из настроек"),
        "queen_spades_value" to Choice(label = "queen_spades_value", description = "Номинал дамы пик из настроек"),
        "king_value" to Choice(label = "king_value", description = "Номинал короля из настроек"),
        "threshold_score" to Choice(label = "threshold_score", description = "Лимит проигрыша из настроек"),
    )

    val actions: Map<String, Choice> = mapOf(
        "subtract_total" to Choice(
            label = "Вычесть итог",
            description = "delta = -итог_игрока (обнуляет счёт, если в условии total_score >= N).",
        ),
    )
}