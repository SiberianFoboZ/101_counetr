package com.counter.game.engine

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Сериализация UI-формы правила в `definition_json` и обратно.
 *
 * Сторона формы — это [Form] / [NominalForm] / [SuitForm] / [ConditionForm] / [ActionForm],
 * максимально приближённые к тому, что пользователь видит на экране. Сторона движка —
 * [RuleDefinition.Rule]. Парсер уже существует; зеркалим его здесь.
 */
object RuleSerializer {

    // region Form (UI-facing)

    data class Form(
        val kind: Kind = Kind.PER_CARD,
        val nominal: NominalForm = NominalForm.AnyNominal,
        val suit: SuitForm = SuitForm.ANY,
        val condition: ConditionForm? = null,
        val then: ActionForm = ActionForm.BaseValue,
        val elseAction: ActionForm = ActionForm.BaseValue,
    )

    enum class Kind(val stored: String) {
        PER_CARD("per_card"),
        FINAL_ADJUSTMENT("final_adjustment"),
        ;

        companion object {
            fun fromStored(value: String): Kind =
                entries.firstOrNull { it.stored == value } ?: PER_CARD
        }
    }

    sealed interface NominalForm {
        data object AnyNominal : NominalForm
        data class Eq(val value: Int) : NominalForm
        data class Ne(val value: Int) : NominalForm
        data class In(val values: List<Int>) : NominalForm
    }

    enum class SuitForm(val stored: String) {
        ANY("any"),
        SPADES("spades"),
        HEARTS("hearts"),
        DIAMONDS("diamonds"),
        CLUBS("clubs"),
        RED("red"),
        BLACK("black"),
        NON_SPADES("non_spades"),
        ;

        companion object {
            fun fromStored(value: String): SuitForm =
                entries.firstOrNull { it.stored == value.lowercase() } ?: ANY
        }
    }

    enum class OperandForm(val stored: String) {
        CARD_COUNT("card_count"),
        ROUND_DELTA_SO_FAR("round_delta_so_far"),
        DISTINCT_CARD_CODES("distinct_card_codes"),
        TOTAL_SCORE("total_score"),
        ;

        companion object {
            fun fromStored(value: String): OperandForm =
                entries.firstOrNull { it.stored == value } ?: CARD_COUNT
        }
    }

    enum class OpForm(val stored: String) {
        EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">="),
        ;

        companion object {
            fun fromStored(value: String): OpForm =
                entries.firstOrNull { it.stored == value } ?: EQ
        }
    }

    data class ConditionForm(
        val op: OpForm = OpForm.EQ,
        val left: OperandForm = OperandForm.CARD_COUNT,
        val rightValue: Int = 0,
    )

    sealed interface ActionForm {
        data class Const(val value: Int) : ActionForm
        data object BaseValue : ActionForm
        data class Setting(val key: String) : ActionForm
        /** delta = -totalScore (только для FINAL_ADJUSTMENT). */
        data object SubtractTotal : ActionForm
    }

    // endregion

    // region form → JSON

    fun encode(form: Form): String = buildJsonObject {
        put("version", RuleDefinition.VERSION)
        put("kind", form.kind.stored)
        put("match", buildJsonObject {
            put("nominal", nominalToJson(form.nominal))
            put("suit", form.suit.stored)
            form.condition?.let { put("condition", conditionToJson(it)) }
        })
        put("then", actionToJson(form.then))
        put("else", actionToJson(form.elseAction))
    }.toString()

    private fun nominalToJson(form: NominalForm): JsonObject = when (form) {
        NominalForm.AnyNominal -> buildJsonObject { put("op", "any") }
        is NominalForm.Eq -> buildJsonObject { put("op", "=="); put("value", form.value) }
        is NominalForm.Ne -> buildJsonObject { put("op", "!="); put("value", form.value) }
        is NominalForm.In -> buildJsonObject {
            put("op", "in")
            put("values", buildJsonArray { form.values.forEach { add(JsonPrimitive(it)) } })
        }
    }

    private fun conditionToJson(form: ConditionForm): JsonObject = buildJsonObject {
        put("op", form.op.stored)
        put("left", form.left.stored)
        put("right", buildJsonObject {
            put("type", "const")
            put("value", form.rightValue)
        })
    }

    private fun actionToJson(form: ActionForm): JsonObject = when (form) {
        is ActionForm.Const -> buildJsonObject { put("type", "const"); put("value", form.value) }
        ActionForm.BaseValue -> buildJsonObject { put("type", "base_value") }
        is ActionForm.Setting -> buildJsonObject { put("type", "setting"); put("key", form.key) }
        ActionForm.SubtractTotal -> buildJsonObject { put("type", "subtract_total") }
    }

    // endregion

    // region JSON → form

    fun decode(raw: String): Form? {
        val rule = RuleDefinition.parseOrNull(raw) ?: return null
        return Form(
            kind = Kind.fromStored(rule.kind.name.lowercase()),
            nominal = nominalFromRule(rule.match.nominal),
            suit = SuitForm.fromStored(rule.match.suit.name.lowercase()),
            condition = rule.match.condition?.let(::conditionFromRule),
            then = actionFromRule(rule.then),
            elseAction = actionFromRule(rule.elseAction),
        )
    }

    private fun nominalFromRule(p: RuleDefinition.NominalPredicate): NominalForm = when (p) {
        RuleDefinition.NominalPredicate.Any -> NominalForm.AnyNominal
        is RuleDefinition.NominalPredicate.Eq -> NominalForm.Eq(p.value)
        is RuleDefinition.NominalPredicate.Ne -> NominalForm.Ne(p.value)
        is RuleDefinition.NominalPredicate.In -> NominalForm.In(p.values)
    }

    private fun conditionFromRule(c: RuleDefinition.Condition): ConditionForm {
        val rightValue = when (val r = c.right) {
            is RuleDefinition.Operand.Const -> r.value
            else -> 0
        }
        val leftKey = when (val l = c.left) {
            RuleDefinition.Operand.CardCount -> "card_count"
            RuleDefinition.Operand.RoundDeltaSoFar -> "round_delta_so_far"
            RuleDefinition.Operand.DistinctCardCodes -> "distinct_card_codes"
            RuleDefinition.Operand.TotalScore -> "total_score"
            else -> "card_count"
        }
        val opStored = when (c.op) {
            RuleDefinition.Op.EQ -> "=="
            RuleDefinition.Op.NE -> "!="
            RuleDefinition.Op.LT -> "<"
            RuleDefinition.Op.LE -> "<="
            RuleDefinition.Op.GT -> ">"
            RuleDefinition.Op.GE -> ">="
        }
        return ConditionForm(
            op = OpForm.fromStored(opStored),
            left = OperandForm.fromStored(leftKey),
            rightValue = rightValue,
        )
    }

    private fun actionFromRule(a: RuleDefinition.Action): ActionForm = when (a) {
        is RuleDefinition.Action.Const -> ActionForm.Const(a.value)
        RuleDefinition.Action.BaseValue -> ActionForm.BaseValue
        is RuleDefinition.Action.Setting -> ActionForm.Setting(a.key)
        RuleDefinition.Action.SubtractTotal -> ActionForm.SubtractTotal
    }

    // endregion
}