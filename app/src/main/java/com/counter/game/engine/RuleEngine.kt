package com.counter.game.engine

import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.data.entity.RuleEntity
import com.counter.game.data.entity.SettingsEntity
import com.counter.game.data.entity.RoundCardEntity

/**
 * Карты игрока в одном раунде. Удобно для тестов и движка — без зависимости от Room-DAO.
 */
data class RoundHand(
    val cards: List<RoundCardEntity>,
)

/**
 * Контекст для [RuleEngine]: всё, что нужно для одного раунда одного игрока.
 */
data class RuleContext(
    val settings: SettingsEntity,
    val cardDefs: Map<String, CardDefinitionEntity>,
    val hand: RoundHand,
)

/**
 * Возвращает deltaScore (может быть отрицательным).
 */
class RuleEngine {
    /**
     * Алгоритм по ТЗ, раздел 2.6:
     * 1) сортируем включённые правила по priority DESC;
     * 2) для каждого правила: проверяем match.nominal, match.suit, match.condition;
     * 3) применяем then или else, домножая base_value на card_count;
     * 4) складываем всё в deltaScore.
     */
    fun compute(rules: List<RuleEntity>, ctx: RuleContext): Int {
        var delta = 0
        var soFar = 0
        val sorted = rules.filter { it.enabled }.sortedByDescending { it.priority }
        for (rule in sorted) {
            val def = RuleDefinition.parseOrNull(rule.definitionJson) ?: continue
            // Базовая "card_count" — по applies_to_card
            val handCount = ctx.hand.cards.filter { it.cardCode == rule.appliesToCard }
                .sumOf { it.count }
            if (handCount == 0) continue

            // Если applies_to_card — конкретный код (например "K_spades"), проверяем match.suit/nominal
            // против baseValue этого кода и масти, вытащенной из кода.
            val cardDef = ctx.cardDefs[rule.appliesToCard] ?: continue
            val suitFromCode = suitOf(rule.appliesToCard)

            // match.nominal
            val nominalOk = when (val n = def.match.nominal) {
                RuleDefinition.NominalPredicate.Any -> true
                is RuleDefinition.NominalPredicate.Eq -> n.value == cardDef.baseValue
                is RuleDefinition.NominalPredicate.Ne -> n.value != cardDef.baseValue
                is RuleDefinition.NominalPredicate.In -> cardDef.baseValue in n.values
            }
            if (!nominalOk) continue

            // match.suit
            val suitOk = when (def.match.suit) {
                RuleDefinition.SuitPredicate.ANY -> true
                RuleDefinition.SuitPredicate.SPADES -> suitFromCode == "spades"
                RuleDefinition.SuitPredicate.HEARTS -> suitFromCode == "hearts"
                RuleDefinition.SuitPredicate.DIAMONDS -> suitFromCode == "diamonds"
                RuleDefinition.SuitPredicate.CLUBS -> suitFromCode == "clubs"
                RuleDefinition.SuitPredicate.RED -> suitFromCode in setOf("hearts", "diamonds")
                RuleDefinition.SuitPredicate.BLACK -> suitFromCode in setOf("spades", "clubs")
                RuleDefinition.SuitPredicate.NON_SPADES -> suitFromCode != null && suitFromCode != "spades"
            }
            if (!suitOk) continue

            // match.condition
            val condOk = def.match.condition?.let { c ->
                val l = resolveOperand(c.left, ctx, rule.appliesToCard)
                val r = resolveOperand(c.right, ctx, rule.appliesToCard)
                compare(c.op, l, r)
            } ?: true
            if (!condOk) continue

            // then / else
            val action = if (condOk) def.then else def.elseAction
            val value = resolveAction(action, ctx, rule.appliesToCard, handCount)
            delta += value
            soFar += value
        }
        return delta
    }

    private fun resolveOperand(
        op: RuleDefinition.Operand,
        ctx: RuleContext,
        appliesTo: String,
    ): Int = when (op) {
        is RuleDefinition.Operand.Const -> op.value
        RuleDefinition.Operand.CardCount -> ctx.hand.cards
            .filter { it.cardCode == appliesTo }
            .sumOf { it.count }
        RuleDefinition.Operand.RoundDeltaSoFar -> 0 // в пределах одного apply — 0; движок суммирует после
        RuleDefinition.Operand.DistinctCardCodes -> ctx.hand.cards
            .filter { it.count > 0 }
            .map { it.cardCode }
            .distinct()
            .size
    }

    private fun resolveAction(
        action: RuleDefinition.Action,
        ctx: RuleContext,
        appliesTo: String,
        cardCount: Int,
    ): Int = when (action) {
        is RuleDefinition.Action.Const -> action.value
        RuleDefinition.Action.BaseValue -> {
            val def = ctx.cardDefs[appliesTo] ?: return 0
            def.baseValue * cardCount
        }
        is RuleDefinition.Action.Setting -> when (action.key) {
            "queen_value" -> ctx.settings.queenValue
            "queen_spades_value" -> ctx.settings.queenSpadesValue
            "king_value" -> ctx.settings.kingValue
            "threshold_score" -> ctx.settings.thresholdScore
            else -> 0
        }
    }

    private fun compare(op: RuleDefinition.Op, l: Int, r: Int): Boolean = when (op) {
        RuleDefinition.Op.EQ -> l == r
        RuleDefinition.Op.NE -> l != r
        RuleDefinition.Op.LT -> l < r
        RuleDefinition.Op.LE -> l <= r
        RuleDefinition.Op.GT -> l > r
        RuleDefinition.Op.GE -> l >= r
    }

    private fun suitOf(code: String): String? = when {
        code.endsWith("_spades") -> "spades"
        code.endsWith("_hearts") -> "hearts"
        code.endsWith("_diamonds") -> "diamonds"
        code.endsWith("_clubs") -> "clubs"
        else -> null
    }
}