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
    /**
     * Итоговый счёт игрока до раунда. Используется FINAL_ADJUSTMENT правилами.
     * Если не передано — считается 0 (полезно для превью в редакторе правил).
     */
    val totalScoreBeforeRound: Int = 0,
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
        val sorted = rules.filter { it.enabled }.sortedByDescending { it.priority }

        // Фаза 1: per-card правила.
        var rawDelta = 0
        val perCard = sorted.mapNotNull { rule ->
            val def = RuleDefinition.parseOrNull(rule.definitionJson) ?: return@mapNotNull null
            if (def.kind != RuleDefinition.Kind.PER_CARD) return@mapNotNull null
            rule to def
        }
        for ((rule, def) in perCard) {
            val handCount = ctx.hand.cards.filter { it.cardCode == rule.appliesToCard }
                .sumOf { it.count }
            if (handCount == 0) continue

            val cardDef = ctx.cardDefs[rule.appliesToCard] ?: continue
            val suitFromCode = suitOf(rule.appliesToCard)

            val nominalOk = when (val n = def.match.nominal) {
                RuleDefinition.NominalPredicate.Any -> true
                is RuleDefinition.NominalPredicate.Eq -> n.value == cardDef.baseValue
                is RuleDefinition.NominalPredicate.Ne -> n.value != cardDef.baseValue
                is RuleDefinition.NominalPredicate.In -> cardDef.baseValue in n.values
            }
            if (!nominalOk) continue

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

            val condOk = def.match.condition?.let { c ->
                val l = resolveOperand(c.left, ctx, appliesTo = rule.appliesToCard)
                val r = resolveOperand(c.right, ctx, appliesTo = rule.appliesToCard)
                compare(c.op, l, r)
            } ?: true

            val action = if (condOk) def.then else def.elseAction
            val value = resolveAction(action, ctx, rule.appliesToCard, handCount)
            rawDelta += value
        }

        // Фаза 2: final-adjustment правила (например «обнулить при total ≥ 101»).
        val totalAfter = ctx.totalScoreBeforeRound + rawDelta
        val finalAdjustments = sorted.mapNotNull { rule ->
            val def = RuleDefinition.parseOrNull(rule.definitionJson) ?: return@mapNotNull null
            if (def.kind != RuleDefinition.Kind.FINAL_ADJUSTMENT) return@mapNotNull null
            rule to def
        }
        var finalDelta = 0
        for ((_, def) in finalAdjustments) {
            val condOk = def.match.condition?.let { c ->
                val l = resolveOperand(c.left, ctx, appliesTo = null, totalOverride = totalAfter)
                val r = resolveOperand(c.right, ctx, appliesTo = null, totalOverride = totalAfter)
                compare(c.op, l, r)
            } ?: true
            // Финальные правила: без аплиса и без карточной арифметики. Просто then.
            if (!condOk) continue
            val value = resolveFinalAction(def.then, totalAfter)
            finalDelta += value
        }

        return rawDelta + finalDelta
    }

    private fun resolveFinalAction(action: RuleDefinition.Action, totalAfter: Int): Int =
        when (action) {
            is RuleDefinition.Action.Const -> action.value
            RuleDefinition.Action.BaseValue -> 0
            is RuleDefinition.Action.Setting -> 0
            RuleDefinition.Action.SubtractTotal -> -totalAfter
        }

    private fun resolveOperand(
        op: RuleDefinition.Operand,
        ctx: RuleContext,
        appliesTo: String?,
        totalOverride: Int? = null,
    ): Int = when (op) {
        is RuleDefinition.Operand.Const -> op.value
        RuleDefinition.Operand.CardCount -> {
            val code = appliesTo ?: return 0
            ctx.hand.cards.filter { it.cardCode == code }.sumOf { it.count }
        }
        RuleDefinition.Operand.RoundDeltaSoFar -> 0
        RuleDefinition.Operand.DistinctCardCodes -> ctx.hand.cards
            .filter { it.count > 0 }
            .map { it.cardCode }
            .distinct()
            .size
        RuleDefinition.Operand.TotalScore -> totalOverride
            ?: (ctx.totalScoreBeforeRound)
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
        RuleDefinition.Action.SubtractTotal -> 0 // только в final-adjustment
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